package com.syncplatform.shopify_connector_service.controller;

import com.syncplatform.shopify_connector_service.entity.PlatformAccount;
import com.syncplatform.shopify_connector_service.service.ShopifyOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/shopify/oauth")
@RequiredArgsConstructor
public class ShopifyOAuthController {

    private final ShopifyOAuthService shopifyOAuthService;

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(
            @RequestParam String shop,
            @AuthenticationPrincipal Jwt jwt) {

        String state = UUID.randomUUID().toString();
        String authUrl = shopifyOAuthService.buildAuthorizationUrl(shop, state);
        return ResponseEntity.ok(Map.of(
                "authorizationUrl", authUrl,
                "state", state));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String shop,
            @RequestParam String state,
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        PlatformAccount account = shopifyOAuthService.handleCallback(
                userId, shop, code, state);

        return ResponseEntity.ok(Map.of(
                "message", "Shopify connected successfully",
                "shopName", account.getDisplayName(),
                "accountId", account.getId().toString()));
    }
}