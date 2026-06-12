package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.entity.SyncConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SyncConnectionRepository extends JpaRepository<SyncConnection, UUID> {

    @Query("SELECT sc FROM SyncConnection sc WHERE sc.userId = :userId AND sc.deletedAt IS NULL")
    List<SyncConnection> findActiveConnectionsByUserId(UUID userId);

    long countByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<SyncConnection> findByIdAndUserId(UUID id, UUID userId);
}