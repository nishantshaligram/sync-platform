package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.entity.SyncRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface SyncRunRepository extends JpaRepository<SyncRun, UUID> {
    Page<SyncRun> findBySyncConnectionIdOrderByStartedAtDesc(
            UUID syncConnectionId, Pageable pageable);
}