package com.syncplatform.sync_core_service.controller;

import com.syncplatform.sync_core_service.dto.*;
import com.syncplatform.sync_core_service.service.SyncConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/connections")
@RequiredArgsConstructor
public class SyncConnectionController {

    private final SyncConnectionService syncConnectionService;

    @PostMapping
    public ResponseEntity<ConnectionResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateConnectionRequest request) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(
                syncConnectionService.createConnection(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<ConnectionResponse>> list(
            @AuthenticationPrincipal Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(syncConnectionService.listConnections(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConnectionResponse> get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(syncConnectionService.getConnection(userId, id));
    }

    @PostMapping("/{id}/sync")
    public ResponseEntity<?> manualSync(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = UUID.fromString(jwt.getSubject());
        Map<String, Object> result = syncConnectionService.triggerManualSync(userId, id);

        if ("locked".equals(result.get("error"))) {
            return ResponseEntity.status(409).body(result);
        }
        if ("quota_exceeded".equals(result.get("error"))) {
            return ResponseEntity.status(429).body(result);
        }

        return ResponseEntity.accepted().body(result);
    }
}