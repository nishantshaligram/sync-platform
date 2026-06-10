package com.syncplatform.shopify_connector_service.document;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "raw_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawWebhookEvent {

    @Id
    private String id;
    private String syncConnectedId;
    private String platform;
    private String eventType;
    private String externalEventId;
    private Instant receivedAt;
    private boolean signatureVerifeid;
    private Map<String, Object> rawPayload;
    private ProcessingInfo processing;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessingInfo {
        private String status;
        private String syncRunId;
        private Instant processedAt;
        private String error;
    }
}
