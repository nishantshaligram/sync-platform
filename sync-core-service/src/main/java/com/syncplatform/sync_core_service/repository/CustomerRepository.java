package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findBySyncConnectionIdAndEmail(UUID syncConnectionId, String email);

    Optional<Customer> findBySyncConnectionIdAndExternalCustomerId(
            UUID syncConnectionId, String externalCustomerId);
}