package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.entity.SyncSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SyncScheduleRepository extends JpaRepository<SyncSchedule, UUID> {
    Optional<SyncSchedule> findBySyncConnectionId(UUID syncConnectionId);
}