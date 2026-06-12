package com.syncplatform.sync_core_service.repository;

import com.syncplatform.sync_core_service.document.RawWebhookEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RawWebhookEventRepository extends MongoRepository<RawWebhookEvent, String> {
}