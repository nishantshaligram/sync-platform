package com.syncplatform.sync_core_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class QboCommand {
    private String commandId;
    private String syncRunId;
    private String syncConnectionId;
    private String commandType;
    private String canonicalEntityId;
    private String canonicalEntityType;
    private List<String> dependsOn;
    private String issuedAt;
    private Object payload;
}