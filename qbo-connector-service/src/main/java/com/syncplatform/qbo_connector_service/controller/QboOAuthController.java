package com.syncplatform.qbo_connector_service.controller;

import com.syncplatform.qbo_connector_service.entity.PlatformAccount;
import com.syncplatform.qbo_connector_service.service.QboOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/qbo/oauth")
@RequiredArgsConstructor
public class QboOAuthController {

    private final QboOAuthService qboOAuthService;

    @GetMapping("/authorize")
    public ResponseEntity<?> authorize(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String authUrl = qboOAuthService.buildAuthorizationUrl(userId);
        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(value = "realmId") String realmId) {

        PlatformAccount account = qboOAuthService.handleCallback(
            code, state, realmId);

        return ResponseEntity.ok(Map.of(
            "message", "QBO connected successfully",
            "companyName", account.getDisplayName(),
            "accountId", account.getId().toString(),
            "realmId", realmId
        ));
    }
}