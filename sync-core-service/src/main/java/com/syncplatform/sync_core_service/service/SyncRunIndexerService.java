package com.syncplatform.sync_core_service.service;

import com.syncplatform.sync_core_service.document.SyncRunDocument;
import com.syncplatform.sync_core_service.entity.SyncConnection;
import com.syncplatform.sync_core_service.entity.SyncRun;
import com.syncplatform.sync_core_service.repository.SyncConnectionRepository;
import com.syncplatform.sync_core_service.repository.SyncRunDocumentRepository;
import com.syncplatform.sync_core_service.repository.SyncRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SyncRunIndexerService {

    private final SyncRunRepository syncRunRepository;
    private final SyncConnectionRepository syncConnectionRepository;
    private final SyncRunDocumentRepository syncRunDocumentRepository;

    @KafkaListener(topics = "sync.run.completed", groupId = "sync-core-indexer-group")
    public void indexCompletedRun(String runIdStr) {
        try {
            UUID runId = UUID.fromString(runIdStr.replace("\"", ""));

            SyncRun run = syncRunRepository.findById(runId).orElse(null);
            if (run == null) {
                log.warn("SyncRun not found for indexing: {}", runId);
                return;
            }

            SyncConnection connection = syncConnectionRepository
                    .findById(run.getSyncConnectionId())
                    .orElse(null);

            long durationMs = 0;
            if (run.getStartedAt() != null && run.getCompletedAt() != null) {
                durationMs = Duration.between(run.getStartedAt(), run.getCompletedAt()).toMillis();
            }

            SyncRunDocument doc = SyncRunDocument.builder()
                    .runId(run.getId().toString())
                    .syncConnectionId(run.getSyncConnectionId().toString())
                    .userId(connection != null ? connection.getUserId().toString() : null)
                    .triggerType(run.getTriggerType())
                    .status(run.getStatus())
                    .errorCategory(run.getErrorCategory())
                    .startedAt(run.getStartedAt())
                    .completedAt(run.getCompletedAt())
                    .durationMs(durationMs)
                    .eventsProcessed(run.getEventsProcessed())
                    .eventsFailed(run.getEventsFailed())
                    .connectionName(connection != null ? connection.getName() : "")
                    .build();

            syncRunDocumentRepository.save(doc);
            log.info("Indexed sync run {} to Elasticsearch", runId);

        } catch (Exception e) {
            log.error("Failed to index sync run: {}", runIdStr, e);
        }
    }
}