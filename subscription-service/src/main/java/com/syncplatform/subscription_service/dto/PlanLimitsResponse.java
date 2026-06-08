package com.syncplatform.subscription_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PlanLimitsResponse {
    private String planCode;
    private int maxConnections;
    private int[] allowedIntervals;
    private int manualSyncPerDay;
    private int backfillDays;
    private int historyRetentionDays;
}