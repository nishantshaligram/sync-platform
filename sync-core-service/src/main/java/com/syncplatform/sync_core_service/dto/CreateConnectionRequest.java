package com.syncplatform.sync_core_service.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class CreateConnectionRequest {
    private String name;
    private UUID sourceAccountId;
    private UUID destinationAccountId;
    private String timezone;
    private int intervalHours;
    private int backfillDays;
}