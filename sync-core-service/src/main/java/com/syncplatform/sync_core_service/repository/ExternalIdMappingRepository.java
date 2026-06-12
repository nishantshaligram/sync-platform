package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.entity.ExternalIdMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalIdMappingRepository extends JpaRepository<ExternalIdMapping, UUID> {

    Optional<ExternalIdMapping> findBySyncConnectionIdAndCanonicalEntityTypeAndCanonicalEntityIdAndPlatform(
            UUID syncConnectionId, String canonicalEntityType, UUID canonicalEntityId, String platform);

    @Query("""
            SELECT m FROM ExternalIdMapping m
            WHERE m.syncConnectionId = :connectionId
              AND m.canonicalEntityType = :entityType
              AND m.canonicalEntityId IN :entityIds
            """)
    List<ExternalIdMapping> findMappings(UUID connectionId, String entityType, List<UUID> entityIds);
}