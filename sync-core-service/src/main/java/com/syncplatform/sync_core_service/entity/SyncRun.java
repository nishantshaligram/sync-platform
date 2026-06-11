package com.syncplatform.sync_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sync_connection_id", nullable = false)
    private UUID syncConnectionId;

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(name = "triggered_by_user_id")
    private UUID triggeredByUserId;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(nullable = false)
    private String status;

    @Column(name = "events_processed")
    private int eventsProcessed;

    @Column(name = "events_failed")
    private int eventsFailed;

    @Column(name = "error_summary")
    private String errorSummary;

    @Column(name = "error_category")
    private String errorCategory;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}