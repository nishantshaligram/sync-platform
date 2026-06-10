package com.syncplatform.shopify_connector_service.repository;

import com.syncplatform.shopify_connector_service.entity.PlatformAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, UUID> {
    Optional<PlatformAccount> findByUserIdAndPlatformAndExternalAccountId(
            UUID userId, String platform, String externalAccountId);

    Optional<PlatformAccount> findByUserIdAndPlatform(UUID userId, String platform);
}