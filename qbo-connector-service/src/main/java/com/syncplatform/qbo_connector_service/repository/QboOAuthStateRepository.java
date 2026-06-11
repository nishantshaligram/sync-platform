package com.syncplatform.qbo_connector_service.repository;

import com.syncplatform.qbo_connector_service.entity.QboOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface QboOAuthStateRepository extends JpaRepository<QboOAuthState, UUID> {
    Optional<QboOAuthState> findByState(String state);
}