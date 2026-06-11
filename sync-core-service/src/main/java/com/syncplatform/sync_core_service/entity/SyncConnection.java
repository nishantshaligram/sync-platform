package com.syncplatform.sync_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sync_connections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SyncConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private UUID destinationAccountId;

    @Column(nullable = false)
    private String status;

    @Column(name = "sync_settings", columnDefinition = "JSONB")
    private String syncSettings;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    @Column(name = "last_successful_sync_at")
    private OffsetDateTime lastSuccessfulSyncAt;

    @Column(name = "last_sync_status")
    private String lastSyncStatus;

    @Column(name = "last_sync_run_id")
    private UUID lastSyncRunId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

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