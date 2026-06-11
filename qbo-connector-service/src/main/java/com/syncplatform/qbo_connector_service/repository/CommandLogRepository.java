package com.syncplatform.qbo_connector_service.repository;

import com.syncplatform.qbo_connector_service.document.CommandLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CommandLogRepository extends MongoRepository<CommandLog, String> {
}