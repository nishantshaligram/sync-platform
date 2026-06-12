package com.syncplatform.sync_core_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncRunRequestedEvent {
    private String syncConnectionId;
    // Trigger types are basically Sync Requested triggered by which action.
    // For example if i have trigered sync manually its manual.
    // if it is scheduled trigger then it is a scheduled.
    // if it is triggered during inital connection.
    private String triggerType;
    // teigered by userID only available if it is manual sync trigger.
    private String triggeredByUserId;
}