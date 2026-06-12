package com.syncplatform.scheduler_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncRunRequestedEvent {
    private String syncConnectionId;
    private String triggerType;
    private String triggeredByUserId;
}