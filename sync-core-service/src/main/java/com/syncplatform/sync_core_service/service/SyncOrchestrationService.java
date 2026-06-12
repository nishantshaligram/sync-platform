package com.syncplatform.sync_core_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncplatform.sync_core_service.document.RawWebhookEvent;
import com.syncplatform.sync_core_service.dto.QboCommand;
import com.syncplatform.sync_core_service.dto.QboCommandResult;
import com.syncplatform.sync_core_service.dto.SyncRunRequestedEvent;
import com.syncplatform.sync_core_service.entity.*;
import com.syncplatform.sync_core_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncOrchestrationService {

    private final SyncRunRepository syncRunRepository;
    private final SyncConnectionRepository syncConnectionRepository;
    private final SyncScheduleRepository syncScheduleRepository;
    private final PendingSyncEventRepository pendingSyncEventRepository;
    private final RawWebhookEventRepository rawWebhookEventRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ExternalIdMappingRepository externalIdMappingRepository;
    private final CanonicalEntityBuilder canonicalEntityBuilder;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ManualSyncQuotaService manualSyncQuotaService;

    private static final int BATCH_SIZE = 5000;
    private static final Duration LOCK_TTL = Duration.ofHours(1);

    @KafkaListener(topics = "sync.run.requested", groupId = "sync-core-group")
    public void consumeRunRequested(SyncRunRequestedEvent event) {
        UUID connectionId = UUID.fromString(event.getSyncConnectionId());
        String lockKey = "sync_lock:" + connectionId;

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", LOCK_TTL);

        if (Boolean.FALSE.equals(acquired)) {
            log.info("Sync lock held for connection {}, dropping run request", connectionId);
            return;
        }

        try {
            processRun(connectionId, event.getTriggerType(), event.getTriggeredByUserId());
        } catch (Exception e) {
            log.error("Sync run failed for connection {}: {}", connectionId, e.getMessage(), e);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Transactional
    public void processRun(UUID connectionId, String triggerType, String triggeredByUserId) {
        SyncRun run = findOrCreateRun(connectionId, triggerType, triggeredByUserId);
        run.setStatus("running");
        run.setStartedAt(OffsetDateTime.now());
        syncRunRepository.save(run);

        log.info("Sync run {} started for connection {}", run.getId(), connectionId);

        List<PendingSyncEvent> events = pendingSyncEventRepository
                .findPendingEvents(connectionId, PageRequest.of(0, BATCH_SIZE));

        if (events.isEmpty()) {
            log.info("No pending events for connection {}", connectionId);
            completeRun(run, 0, 0);
            return;
        }

        log.info("Processing {} pending events for connection {}", events.size(), connectionId);

        int processed = 0;
        int failed = 0;
        List<QboCommand> commandsToEmit = new ArrayList<>();

        for (PendingSyncEvent event : events) {
            try {
                event.setStatus("processing");
                pendingSyncEventRepository.save(event);

                List<QboCommand> commands = applyEvent(connectionId, run.getId(), event);
                commandsToEmit.addAll(commands);

                event.setStatus("processed");
                event.setProcessedAt(OffsetDateTime.now());
                event.setSyncRunId(run.getId());
                pendingSyncEventRepository.save(event);
                processed++;

            } catch (Exception e) {
                log.error("Failed to process event {}: {}", event.getId(), e.getMessage(), e);
                event.setStatus("failed");
                event.setErrorSummary(e.getMessage());
                event.setSyncRunId(run.getId());
                pendingSyncEventRepository.save(event);
                failed++;
            }
        }

        for (QboCommand command : commandsToEmit) {
            kafkaTemplate.send("qbo.commands", connectionId.toString(), command);
        }

        log.info("Emitted {} QBO commands for run {}", commandsToEmit.size(), run.getId());

        if (commandsToEmit.isEmpty()) {
            completeRun(run, processed, failed);
        } else {
            run.setEventsProcessed(processed);
            run.setEventsFailed(failed);
            syncRunRepository.save(run);

            redisTemplate.opsForValue().set(
                    "run_pending_commands:" + run.getId(),
                    String.valueOf(commandsToEmit.size()),
                    Duration.ofHours(2));
        }
    }

    @KafkaListener(topics = "qbo.command.results", groupId = "sync-core-group")
    @Transactional
    public void consumeCommandResult(QboCommandResult result) {
        UUID runId = UUID.fromString(result.getSyncRunId());
        UUID connectionId = UUID.fromString(result.getSyncConnectionId());

        log.info("Received command result: {} status: {}", result.getCommandId(), result.getStatus());

        if ("success".equals(result.getStatus()) && result.getExternalId() != null) {
            updateIdMapping(connectionId, result);
        }

        String counterKey = "run_pending_commands:" + runId;
        Long remaining = redisTemplate.opsForValue().decrement(counterKey);

        SyncRun run = syncRunRepository.findById(runId).orElse(null);
        if (run == null)
            return;

        if ("success".equals(result.getStatus())) {
            run.setEventsProcessed(run.getEventsProcessed());
        } else {
            run.setEventsFailed(run.getEventsFailed() + 1);
        }
        syncRunRepository.save(run);

        if (remaining != null && remaining <= 0) {
            redisTemplate.delete(counterKey);
            int failed = run.getEventsFailed();
            int processed = run.getEventsProcessed();
            completeRun(run, processed, failed);
        }
    }

    private void updateIdMapping(UUID connectionId, QboCommandResult result) {
        UUID canonicalEntityId = UUID.fromString(result.getCanonicalEntityId());
        String entityType = result.getCanonicalEntityType();

        ExternalIdMapping mapping = externalIdMappingRepository
                .findBySyncConnectionIdAndCanonicalEntityTypeAndCanonicalEntityIdAndPlatform(
                        connectionId, entityType, canonicalEntityId, "qbo")
                .orElse(ExternalIdMapping.builder()
                        .syncConnectionId(connectionId)
                        .canonicalEntityType(entityType)
                        .canonicalEntityId(canonicalEntityId)
                        .platform("qbo")
                        .build());

        mapping.setExternalId(result.getExternalId());
        mapping.setSyncStatus("synced");
        mapping.setLastSyncedAt(OffsetDateTime.now());

        externalIdMappingRepository.save(mapping);
    }

    @SuppressWarnings("unchecked")
    private List<QboCommand> applyEvent(UUID connectionId, UUID runId, PendingSyncEvent event) {
        List<QboCommand> commands = new ArrayList<>();

        if (event.getRawEventRef() == null) {
            log.warn("Event {} has no raw payload reference", event.getId());
            return commands;
        }

        RawWebhookEvent rawEvent = rawWebhookEventRepository
                .findById(event.getRawEventRef())
                .orElse(null);

        if (rawEvent == null) {
            log.warn("Raw event not found for ref: {}", event.getRawEventRef());
            return commands;
        }

        Map<String, Object> payload = rawEvent.getRawPayload();

        if (event.getEventType().contains("orders")) {
            commands.addAll(processOrderEvent(connectionId, runId, payload));
        } else if (event.getEventType().contains("customers")) {
            processCustomerEvent(connectionId, payload);
        }

        return commands;
    }

    private List<QboCommand> processOrderEvent(UUID connectionId, UUID runId, Map<String, Object> payload) {
        List<QboCommand> commands = new ArrayList<>();

        Customer customer = canonicalEntityBuilder.buildCustomerFromShopifyPayload(connectionId, payload);
        Customer savedCustomer = null;

        if (customer != null) {
            savedCustomer = upsertCustomer(connectionId, customer);

            boolean customerMapped = externalIdMappingRepository
                    .findBySyncConnectionIdAndCanonicalEntityTypeAndCanonicalEntityIdAndPlatform(
                            connectionId, "customer", savedCustomer.getId(), "qbo")
                    .filter(m -> "synced".equals(m.getSyncStatus()))
                    .isPresent();

            if (!customerMapped) {
                commands.add(buildCommand(connectionId, runId, "create_customer",
                        savedCustomer.getId(), "customer", null));
            }
        }

        Order order = canonicalEntityBuilder.buildOrderFromShopifyPayload(connectionId, payload);
        if (savedCustomer != null) {
            order.setCustomerId(savedCustomer.getId());
        }

        Order savedOrder = upsertOrder(connectionId, order);

        List<OrderLineItem> lineItems = canonicalEntityBuilder.buildLineItemsFromShopifyPayload(payload);
        for (OrderLineItem li : lineItems) {
            li.setOrderId(savedOrder.getId());
        }
        saveLineItems(savedOrder.getId(), lineItems);

        List<String> dependsOn = commands.isEmpty()
                ? Collections.emptyList()
                : List.of(commands.get(commands.size() - 1).getCommandId());

        commands.add(buildCommand(connectionId, runId, "create_invoice",
                savedOrder.getId(), "order", dependsOn));

        return commands;
    }

    private void processCustomerEvent(UUID connectionId, Map<String, Object> payload) {
        Customer customer = new Customer();
        customer.setSyncConnectionId(connectionId);
        customer.setExternalCustomerId(String.valueOf(payload.get("id")));
        customer.setEmail((String) payload.get("email"));
        customer.setFirstName((String) payload.get("first_name"));
        customer.setLastName((String) payload.get("last_name"));
        customer.setPhone((String) payload.get("phone"));
        customer.setCurrency("USD");
        customer.setTaxExempt(Boolean.TRUE.equals(payload.get("tax_exempt")));

        upsertCustomer(connectionId, customer);
    }

    private Customer upsertCustomer(UUID connectionId, Customer customer) {
        Optional<Customer> existing = Optional.empty();

        if (customer.getExternalCustomerId() != null) {
            existing = customerRepository.findBySyncConnectionIdAndExternalCustomerId(
                    connectionId, customer.getExternalCustomerId());
        }
        if (existing.isEmpty() && customer.getEmail() != null) {
            existing = customerRepository.findBySyncConnectionIdAndEmail(
                    connectionId, customer.getEmail());
        }

        if (existing.isPresent()) {
            Customer toUpdate = existing.get();
            toUpdate.setFirstName(customer.getFirstName());
            toUpdate.setLastName(customer.getLastName());
            toUpdate.setPhone(customer.getPhone());
            toUpdate.setEmail(customer.getEmail());
            return customerRepository.save(toUpdate);
        }

        return customerRepository.save(customer);
    }

    private Order upsertOrder(UUID connectionId, Order order) {
        Optional<Order> existing = orderRepository
                .findBySyncConnectionIdAndExternalOrderId(connectionId, order.getExternalOrderId());

        if (existing.isPresent()) {
            Order toUpdate = existing.get();
            toUpdate.setStatus(order.getStatus());
            toUpdate.setTotalAmount(order.getTotalAmount());
            toUpdate.setCustomerId(order.getCustomerId());
            toUpdate.setPaidAt(order.getPaidAt());
            return orderRepository.save(toUpdate);
        }

        return orderRepository.save(order);
    }

    private void saveLineItems(UUID orderId, List<OrderLineItem> lineItems) {
        for (OrderLineItem li : lineItems) {
            li.setOrderId(orderId);
        }
    }

    private QboCommand buildCommand(UUID connectionId, UUID runId, String commandType,
            UUID entityId, String entityType, List<String> dependsOn) {
        QboCommand command = new QboCommand();
        command.setCommandId(UUID.randomUUID().toString());
        command.setSyncRunId(runId.toString());
        command.setSyncConnectionId(connectionId.toString());
        command.setCommandType(commandType);
        command.setCanonicalEntityId(entityId.toString());
        command.setCanonicalEntityType(entityType);
        command.setDependsOn(dependsOn != null ? dependsOn : Collections.emptyList());
        command.setIssuedAt(OffsetDateTime.now().toString());
        return command;
    }

    private SyncRun findOrCreateRun(UUID connectionId, String triggerType, String triggeredByUserId) {
        return syncRunRepository.findBySyncConnectionIdOrderByStartedAtDesc(
                connectionId, PageRequest.of(0, 10))
                .stream()
                .filter(r -> "queued".equals(r.getStatus()) && triggerType.equals(r.getTriggerType()))
                .findFirst()
                .orElseGet(() -> {
                    SyncRun run = SyncRun.builder()
                            .syncConnectionId(connectionId)
                            .triggerType(triggerType)
                            .triggeredByUserId(triggeredByUserId != null
                                    ? UUID.fromString(triggeredByUserId)
                                    : null)
                            .status("queued")
                            .build();
                    return syncRunRepository.save(run);
                });
    }

    private void completeRun(SyncRun run, int processed, int failed) {
        run.setEventsProcessed(processed);
        run.setEventsFailed(failed);
        run.setCompletedAt(OffsetDateTime.now());

        if (failed == 0) {
            run.setStatus("completed");
        } else if (processed > 0) {
            run.setStatus("partial");
        } else {
            run.setStatus("failed");
        }

        syncRunRepository.save(run);

        if ("manual".equals(run.getTriggerType()) && "failed".equals(run.getStatus())) {
            manualSyncQuotaService.refund(run.getSyncConnectionId());
        }

        SyncConnection conn = syncConnectionRepository.findById(run.getSyncConnectionId()).orElse(null);
        if (conn != null) {
            conn.setLastSyncAt(run.getCompletedAt());
            conn.setLastSyncStatus(run.getStatus());
            conn.setLastSyncRunId(run.getId());
            if ("completed".equals(run.getStatus())) {
                conn.setLastSuccessfulSyncAt(run.getCompletedAt());
            }
            syncConnectionRepository.save(conn);
        }

        syncScheduleRepository.findBySyncConnectionId(run.getSyncConnectionId())
                .ifPresent(schedule -> {
                    OffsetDateTime nextRun = computeNextRun(
                            schedule.getTimezone(), schedule.getIntervalHours(),
                            schedule.getStaggerOffsetMinutes());
                    schedule.setNextRunAtUtc(nextRun);
                    schedule.setLastRunAtUtc(OffsetDateTime.now());
                    syncScheduleRepository.save(schedule);
                });

        kafkaTemplate.send("sync.run.completed", run.getSyncConnectionId().toString(), run.getId().toString());

        log.info("Sync run {} completed with status {}", run.getId(), run.getStatus());
    }

    private OffsetDateTime computeNextRun(String timezone, int intervalHours, int staggerOffset) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime todayAnchor = now.toLocalDate().atStartOfDay(zone)
                .plusMinutes(staggerOffset);

        ZonedDateTime next = todayAnchor;
        while (!next.isAfter(now)) {
            next = next.plusHours(intervalHours);
        }

        return next.withZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime();
    }
}