package com.syncplatform.subscription_service.controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.syncplatform.subscription_service.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final SubscriptionService subscriptionService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        switch (event.getType()) {
            case "customer.subscription.created":
            case "customer.subscription.updated": {
                Subscription subscription = (Subscription)
                        event.getDataObjectDeserializer()
                             .getObject()
                             .orElseThrow();

                String userId = subscription.getMetadata().get("user_id");
                if (userId != null) {
                    subscriptionService.handleSubscriptionCreated(
                            subscription.getId(),
                            subscription.getItems().getData().get(0).getPrice().getId(),
                            subscription.getCustomer(),
                            userId,
                            subscription.getCurrentPeriodStart(),
                            subscription.getCurrentPeriodEnd()
                    );
                }
                break;
            }
            default:
                log.debug("Unhandled Stripe event: {}", event.getType());
        }

        return ResponseEntity.ok("received");
    }
}