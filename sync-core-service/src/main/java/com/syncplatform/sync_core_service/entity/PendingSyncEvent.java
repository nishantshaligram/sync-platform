package com.syncplatform.sync_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_sync_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PendingSyncEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sync_connection_id", nullable = false)
    private UUID syncConnectionId;

    @Column(name = "event_source", nullable = false)
    private String eventSource;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;

    @Column(name = "raw_event_ref")
    private String rawEventRef;

    @Column(name = "received_at")
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "sync_run_id")
    private UUID syncRunId;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_summary")
    private String errorSummary;

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = OffsetDateTime.now();
        }
    }
}