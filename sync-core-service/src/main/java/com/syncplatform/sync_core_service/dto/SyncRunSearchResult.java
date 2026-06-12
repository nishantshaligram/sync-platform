package com.syncplatform.sync_core_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
@AllArgsConstructor
public class SyncRunSearchResult {
    private String runId;
    private String triggerType;
    private String status;
    private String errorCategory;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private long durationMs;
    private int eventsProcessed;
    private int eventsFailed;
}