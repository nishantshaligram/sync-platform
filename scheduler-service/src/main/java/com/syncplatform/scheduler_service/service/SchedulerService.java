package com.syncplatform.scheduler_service.service;

import com.syncplatform.scheduler_service.dto.SyncRunRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int BATCH_LIMIT = 1000;

    @Scheduled(fixedRate = 60000) // every 60 seconds
    @Transactional
    public void pollAndTriggerDueSchedules() {
        String sql = """
            SELECT id, sync_connection_id, interval_hours, timezone, stagger_offset_minutes
            FROM sync_schedules
            WHERE status = 'active' AND next_run_at_utc <= NOW()
            ORDER BY next_run_at_utc
            LIMIT ?
            FOR UPDATE SKIP LOCKED
            """;

        List<Map<String, Object>> dueSchedules = jdbcTemplate.queryForList(sql, BATCH_LIMIT);

        if (dueSchedules.isEmpty()) {
            log.debug("No due schedules found");
            return;
        }

        log.info("Found {} due schedules", dueSchedules.size());

        for (Map<String, Object> schedule : dueSchedules) {
            try {
                processSchedule(schedule);
            } catch (Exception e) {
                log.error("Failed to process schedule {}: {}",
                    schedule.get("id"), e.getMessage(), e);
            }
        }
    }

    private void processSchedule(Map<String, Object> schedule) {
        UUID scheduleId = (UUID) schedule.get("id");
        UUID connectionId = (UUID) schedule.get("sync_connection_id");
        int intervalHours = (int) schedule.get("interval_hours");
        String timezone = (String) schedule.get("timezone");
        int staggerOffset = (int) schedule.get("stagger_offset_minutes");

        // Emit sync.run.requested event
        SyncRunRequestedEvent event = new SyncRunRequestedEvent(
            connectionId.toString(), "scheduled", null);

        kafkaTemplate.send("sync.run.requested", connectionId.toString(), event);

        // Compute next run time
        OffsetDateTime nextRun = computeNextRun(timezone, intervalHours, staggerOffset);

        String updateSql = """
            UPDATE sync_schedules
            SET next_run_at_utc = ?, last_run_at_utc = NOW(), updated_at = NOW()
            WHERE id = ?
            """;

        jdbcTemplate.update(updateSql, Timestamp.from(nextRun.toInstant()), scheduleId);

        log.info("Triggered scheduled sync for connection {}, next run at {}",
            connectionId, nextRun);
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