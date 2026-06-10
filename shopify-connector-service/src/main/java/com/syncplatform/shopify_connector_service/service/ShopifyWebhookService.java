package com.syncplatform.shopify_connector_service.service;

import com.syncplatform.shopify_connector_service.document.RawWebhookEvent;
import com.syncplatform.shopify_connector_service.repository.RawWebhookEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopifyWebhookService {

    private final RawWebhookEventRepository rawWebhookEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${shopify.client-secret}")
    private String clientSecret;

    public boolean verifyHmac(String payload, String hmacHeader) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    clientSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            String computed = Base64.getEncoder().encodeToString(hash);
            return computed.equals(hmacHeader);
        } catch (Exception e) {
            log.error("HMAC verification failed", e);
            return false;
        }
    }

    public void processWebhook(String syncConnectionId, String eventType,
            String webhookId, String payload) throws Exception {
        // Dedup check via Redis
        String idempotencyKey = "idem:shopify:" + webhookId;
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "1", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isNew)) {
            log.info("Duplicate webhook ignored: {}", webhookId);
            return;
        }

        // Parse payload
        Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);

        // Persist to MongoDB
        RawWebhookEvent event = RawWebhookEvent.builder()
                .syncConnectionId(syncConnectionId)
                .platform("shopify")
                .eventType(eventType)
                .externalEventId(webhookId)
                .receivedAt(Instant.now())
                .signatureVerified(true)
                .rawPayload(payloadMap)
                .processing(RawWebhookEvent.ProcessingInfo.builder()
                        .status("pending")
                        .build())
                .build();

        rawWebhookEventRepository.save(event);

        // Publish to Kafka
        kafkaTemplate.send("shopify.webhooks.raw", syncConnectionId, event);
        log.info("Webhook published to Kafka: {} for connection: {}",
                eventType, syncConnectionId);
    }
}