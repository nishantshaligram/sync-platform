package com.syncplatform.sync_core_service.service;

import com.syncplatform.sync_core_service.dto.*;
import com.syncplatform.sync_core_service.entity.*;
import com.syncplatform.sync_core_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncConnectionService {

        private final SyncConnectionRepository syncConnectionRepository;
        private final SyncScheduleRepository syncScheduleRepository;
        private final SyncRunRepository syncRunRepository;
        private final KafkaTemplate<String, Object> kafkaTemplate;
        private final ManualSyncQuotaService manualSyncQuotaService;
        private final SyncLockService syncLockService;

        @Transactional
        public ConnectionResponse createConnection(UUID userId, CreateConnectionRequest request) {

                SyncConnection connection = SyncConnection.builder()
                                .userId(userId)
                                .name(request.getName())
                                .sourceAccountId(request.getSourceAccountId())
                                .destinationAccountId(request.getDestinationAccountId())
                                .status("setup_pending")
                                .syncSettings("{\"entities\": [\"customers\", \"orders\"]}")
                                .build();

                connection = syncConnectionRepository.save(connection);

                // Compute initial next_run_at_utc — now + interval
                int staggerOffset = Math.abs(connection.getId().hashCode()) % 60;
                OffsetDateTime nextRun = computeNextRun(
                                request.getTimezone(), request.getIntervalHours(), staggerOffset);

                SyncSchedule schedule = SyncSchedule.builder()
                                .syncConnectionId(connection.getId())
                                .intervalHours(request.getIntervalHours())
                                .timezone(request.getTimezone())
                                .staggerOffsetMinutes(staggerOffset)
                                .nextRunAtUtc(nextRun)
                                .status("active")
                                .build();

                syncScheduleRepository.save(schedule);

                connection.setStatus("active");
                syncConnectionRepository.save(connection);

                // Trigger initial sync
                triggerInitialSync(connection.getId(), userId);

                return toResponse(connection, schedule);
        }

        @Transactional
        public void triggerInitialSync(UUID connectionId, UUID userId) {
                SyncRun run = SyncRun.builder()
                                .syncConnectionId(connectionId)
                                .triggerType("initial")
                                .triggeredByUserId(userId)
                                .status("queued")
                                .build();

                run = syncRunRepository.save(run);

                SyncRunRequestedEvent event = new SyncRunRequestedEvent(
                                connectionId.toString(), "initial", userId.toString());

                kafkaTemplate.send("sync.run.requested", connectionId.toString(), event);
                log.info("Initial sync triggered for connection: {}", connectionId);
        }

        public List<ConnectionResponse> listConnections(UUID userId) {
                return syncConnectionRepository.findActiveConnectionsByUserId(userId)
                                .stream()
                                .map(conn -> {
                                        SyncSchedule schedule = syncScheduleRepository
                                                        .findBySyncConnectionId(conn.getId())
                                                        .orElse(null);
                                        return toResponse(conn, schedule);
                                })
                                .toList();
        }

        public ConnectionResponse getConnection(UUID userId, UUID connectionId) {
                SyncConnection conn = syncConnectionRepository
                                .findByIdAndUserId(connectionId, userId)
                                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

                SyncSchedule schedule = syncScheduleRepository
                                .findBySyncConnectionId(conn.getId())
                                .orElse(null);

                return toResponse(conn, schedule);
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

        private ConnectionResponse toResponse(SyncConnection conn, SyncSchedule schedule) {
                return ConnectionResponse.builder()
                                .id(conn.getId())
                                .name(conn.getName())
                                .status(conn.getStatus())
                                .lastSyncAt(conn.getLastSyncAt())
                                .lastSyncStatus(conn.getLastSyncStatus())
                                .nextRunAtUtc(schedule != null ? schedule.getNextRunAtUtc() : null)
                                .intervalHours(schedule != null ? schedule.getIntervalHours() : 0)
                                .timezone(schedule != null ? schedule.getTimezone() : null)
                                .build();
        }

        @Transactional
        public Map<String, Object> triggerManualSync(UUID userId, UUID connectionId) {
                SyncConnection connection = syncConnectionRepository
                                .findByIdAndUserId(connectionId, userId)
                                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));

                if (!"active".equals(connection.getStatus())) {
                        throw new IllegalArgumentException(
                                        "Connection is not active (status: " + connection.getStatus() + ")");
                }

                // Check sync lock
                if (syncLockService.isLocked(connectionId)) {
                        return Map.of(
                                        "error", "locked",
                                        "message", "A sync is already in progress for this connection");
                }

                // Check manual sync quota — MVP hardcodes Pro tier limit (5/day)
                // In production, fetch from subscription-service /subscriptions/limits
                int dailyLimit = 5;
                boolean allowed = manualSyncQuotaService.checkAndIncrement(connectionId, dailyLimit);

                if (!allowed) {
                        return Map.of(
                                        "error", "quota_exceeded",
                                        "message", "Manual sync quota exceeded for today");
                }

                // Create SyncRun with trigger_type=manual
                SyncRun run = SyncRun.builder()
                                .syncConnectionId(connectionId)
                                .triggerType("manual")
                                .triggeredByUserId(userId)
                                .status("queued")
                                .build();

                run = syncRunRepository.save(run);

                SyncRunRequestedEvent event = new SyncRunRequestedEvent(
                                connectionId.toString(), "manual", userId.toString());

                kafkaTemplate.send("sync.run.requested", connectionId.toString(), event);

                log.info("Manual sync triggered for connection: {} by user: {}", connectionId, userId);

                return Map.of(
                                "message", "Sync started",
                                "runId", run.getId().toString());
        }
}