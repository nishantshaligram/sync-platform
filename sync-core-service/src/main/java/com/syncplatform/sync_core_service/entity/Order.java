package com.syncplatform.sync_core_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sync_connection_id", nullable = false)
    private UUID syncConnectionId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(nullable = false)
    private String status;

    @Column(length = 3)
    private String currency;

    private BigDecimal subtotal;

    @Column(name = "total_discount")
    private BigDecimal totalDiscount;

    @Column(name = "total_tax")
    private BigDecimal totalTax;

    @Column(name = "total_shipping")
    private BigDecimal totalShipping;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "placed_at")
    private OffsetDateTime placedAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "payment_method")
    private String paymentMethod;

    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @OneToMany(mappedBy = "orderId", fetch = FetchType.LAZY)
    @Transient
    private List<OrderLineItem> lineItems;

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