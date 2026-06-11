package com.syncplatform.sync_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "external_id_mappings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalIdMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sync_connection_id", nullable = false)
    private UUID syncConnectionId;

    @Column(name = "canonical_entity_type", nullable = false)
    private String canonicalEntityType;

    @Column(name = "canonical_entity_id", nullable = false)
    private UUID canonicalEntityId;

    @Column(nullable = false)
    private String platform;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "external_metadata", columnDefinition = "JSONB")
    private String externalMetadata;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "sync_status", nullable = false)
    private String syncStatus;

    @Column(name = "last_error")
    private String lastError;

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