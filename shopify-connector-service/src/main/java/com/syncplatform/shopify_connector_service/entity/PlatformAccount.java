package com.syncplatform.shopify_connector_service.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "platform_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userID;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "platform_type", nullable = false)
    private String platformType;

    @Column(name = "external_account_id", nullable = false)
    private String externalAccountId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "access_token_encrypted")
    private byte[] accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted")
    private byte[] refreshTokenEncrypted;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "scopes", columnDefinition = "TEXT[]")
    private String[] scopes;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "last_health_check_at")
    private OffsetDateTime lastHealthCheckAt;

    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
