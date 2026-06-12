package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.entity.PendingSyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface PendingSyncEventRepository extends JpaRepository<PendingSyncEvent, UUID> {

    @Query("""
            SELECT e FROM PendingSyncEvent e
            WHERE e.syncConnectionId = :connectionId AND e.status = 'pending'
            ORDER BY e.receivedAt ASC
            """)
    List<PendingSyncEvent> findPendingEvents(UUID connectionId,
            org.springframework.data.domain.Pageable pageable);

    boolean existsBySyncConnectionIdAndExternalEventId(UUID syncConnectionId, String externalEventId);
}