package com.syncplatform.sync_core_service.service;

import com.syncplatform.sync_core_service.document.RawWebhookEvent;
import com.syncplatform.sync_core_service.entity.PendingSyncEvent;
import com.syncplatform.sync_core_service.repository.PendingSyncEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookEventConsumer {

    private final PendingSyncEventRepository pendingSyncEventRepository;

    @KafkaListener(topics = "shopify.webhooks.raw", groupId = "sync-core-group")
    @Transactional
    public void consumeRawWebhook(RawWebhookEvent event) {
        log.info("Consuming raw webhook: {} for connection: {}",
                event.getEventType(), event.getSyncConnectionId());

        UUID connectionId;
        try {
            connectionId = UUID.fromString(event.getSyncConnectionId());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sync_connection_id in webhook, skipping: {}",
                    event.getSyncConnectionId());
            return;
        }

        // Idempotency check — ON CONFLICT DO NOTHING equivalent
        if (pendingSyncEventRepository.existsBySyncConnectionIdAndExternalEventId(
                connectionId, event.getExternalEventId())) {
            log.info("Duplicate event, skipping: {}", event.getExternalEventId());
            return;
        }

        PendingSyncEvent pendingEvent = PendingSyncEvent.builder()
                .syncConnectionId(connectionId)
                .eventSource("webhook")
                .eventType("shopify." + event.getEventType())
                .externalEventId(event.getExternalEventId())
                .rawEventRef(event.getId())
                .receivedAt(event.getReceivedAt() != null
                        ? OffsetDateTime.ofInstant(event.getReceivedAt(), ZoneOffset.UTC)
                        : OffsetDateTime.now())
                .status("pending")
                .build();

        try {
            pendingSyncEventRepository.save(pendingEvent);
            log.info("Pending sync event created: {} for connection: {}",
                    pendingEvent.getEventType(), connectionId);
        } catch (DataIntegrityViolationException e) {
            // Race condition — another instance inserted it first
            log.info("Duplicate event (race), skipping: {}", event.getExternalEventId());
        }
    }
}