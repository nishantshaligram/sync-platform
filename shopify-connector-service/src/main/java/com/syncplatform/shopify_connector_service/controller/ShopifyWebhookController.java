package com.syncplatform.shopify_connector_service.controller;

import com.syncplatform.shopify_connector_service.service.ShopifyWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shopify/webhooks")
@RequiredArgsConstructor
@Slf4j
public class ShopifyWebhookController {

    private final ShopifyWebhookService webhookService;

    @PostMapping
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader("X-Shopify-Hmac-Sha256") String hmacHeader,
            @RequestHeader("X-Shopify-Topic") String topic,
            @RequestHeader("X-Shopify-Webhook-Id") String webhookId,
            @RequestHeader(value = "X-Shopify-Shop-Domain", required = false) String shopDomain,
            @RequestHeader(value = "X-Sync-Connection-Id", required = false) String syncConnectionId,
            @RequestBody String payload) {

        // Verify HMAC signature
        if (!webhookService.verifyHmac(payload, hmacHeader)) {
            log.warn("Invalid HMAC signature for webhook: {}", webhookId);
            return ResponseEntity.status(401).body("Invalid signature");
        }

        try {
            String connectionId = syncConnectionId != null ? syncConnectionId : shopDomain;
            webhookService.processWebhook(connectionId, topic, webhookId, payload);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing webhook: {}", webhookId, e);
            // Return 200 anyway so Shopify doesn't retry
            return ResponseEntity.ok().build();
        }
    }
}