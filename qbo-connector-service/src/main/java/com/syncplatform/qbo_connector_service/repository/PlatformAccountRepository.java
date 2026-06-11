package com.syncplatform.qbo_connector_service.repository;

import com.syncplatform.qbo_connector_service.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, UUID> {
    Optional<PlatformAccount> findByUserIdAndPlatformAndExternalAccountId(
        UUID userId, String platform, String externalAccountId);
    Optional<PlatformAccount> findByExternalAccountId(String externalAccountId);
}