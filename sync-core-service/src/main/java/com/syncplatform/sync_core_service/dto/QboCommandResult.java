package com.syncplatform.sync_core_service.dto;

import lombok.Data;

@Data
public class QboCommandResult {
    private String commandId;
    private String syncRunId;
    private String syncConnectionId;
    private String canonicalEntityId;
    private String canonicalEntityType;
    private String externalId;
    private String status;
    private String errorMessage;
    private String completedAt;
}