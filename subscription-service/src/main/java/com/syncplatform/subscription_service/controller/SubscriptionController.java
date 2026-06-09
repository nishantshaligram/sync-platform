package com.syncplatform.subscription_service.controller;

import com.syncplatform.subscription_service.dto.*;
import com.syncplatform.subscription_service.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CheckoutRequest request) throws Exception {

        UUID userId = UUID.fromString(jwt.getSubject());
        String url = subscriptionService.createCheckoutSession(userId, request);
        return ResponseEntity.ok(Map.of("checkoutUrl", url));
    }

    @GetMapping("/limits")
    public ResponseEntity<PlanLimitsResponse> limits(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(subscriptionService.getPlanLimits(userId));
    }

    @PostMapping("/demo")
    public ResponseEntity<?> activateDemo(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        subscriptionService.createDemoSubscription(userId);
        return ResponseEntity.ok(Map.of("message", "Demo subscription activated"));
    }
}