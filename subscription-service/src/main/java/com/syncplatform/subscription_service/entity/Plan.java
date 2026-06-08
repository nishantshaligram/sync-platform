package com.syncplatform.subscription_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "stripe_price_id")
    private String stripePriceId;

    @Column(name = "price_amount")
    private BigDecimal priceAmount;

    private String currency;

    @Column(name = "billing_interval")
    private String billingInterval;

    @Column(name = "max_connections")
    private int maxConnections;

    @Column(name = "allowed_intervals", columnDefinition = "integer[]")
    private int[] allowedIntervals;

    @Column(name = "manual_sync_per_day")
    private int manualSyncPerDay;

    @Column(name = "backfill_days")
    private int backfillDays;

    @Column(name = "history_retention_days")
    private int historyRetentionDays;

    @Column(name = "searchable_history_days")
    private int searchableHistoryDays;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}