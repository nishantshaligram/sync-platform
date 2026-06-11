package com.syncplatform.sync_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyncSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sync_connection_id", nullable = false, unique = true)
    private UUID syncConnectionId;

    @Column(name = "interval_hours", nullable = false)
    private int intervalHours;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "stagger_offset_minutes")
    private int staggerOffsetMinutes;

    @Column(name = "next_run_at_utc", nullable = false)
    private OffsetDateTime nextRunAtUtc;

    @Column(name = "last_run_at_utc")
    private OffsetDateTime lastRunAtUtc;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}