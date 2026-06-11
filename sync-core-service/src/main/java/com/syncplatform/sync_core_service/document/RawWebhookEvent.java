package com.syncplatform.sync_core_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.Map;

@Document(collection = "raw_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RawWebhookEvent {

    @Id
    private String id;
    private String syncConnectionId;
    private String platform;
    private String eventType;
    private String externalEventId;
    private Instant receivedAt;
    private boolean signatureVerified;
    private Map<String, Object> rawPayload;
    private ProcessingInfo processing;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProcessingInfo {
        private String status;
        private String syncRunId;
        private Instant processedAt;
        private String error;
    }
}