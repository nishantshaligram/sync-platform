package com.syncplatform.qbo_connector_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document(collection = "command_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommandLog {

    @Id
    private String id;
    private String syncConnectionId;
    private String syncRunId;
    private String commandType;
    private int attempt;
    private Map<String, Object> requestPayload;
    private Map<String, Object> responsePayload;
    private int httpStatus;
    private long durationMs;
    private String status;
    private Instant occurredAt;
}