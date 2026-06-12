package com.syncplatform.sync_core_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
public class ConnectionResponse {
    private UUID id;
    private String name;
    private String status;
    private OffsetDateTime lastSyncAt;
    private String lastSyncStatus;
    private OffsetDateTime nextRunAtUtc;
    private int intervalHours;
    private String timezone;
}