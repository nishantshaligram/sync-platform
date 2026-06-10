package com.syncplatform.shopify_connector_service.repository;

import com.syncplatform.shopify_connector_service.document.RawWebhookEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RawWebhookEventRepository extends MongoRepository<RawWebhookEvent, String> {
    boolean existsByExternalEventId(String externalEventId);
}