package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findBySyncConnectionIdAndExternalOrderId(
            UUID syncConnectionId, String externalOrderId);

    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findOrderById(UUID id);
}