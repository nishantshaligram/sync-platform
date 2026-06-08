package com.syncplatform.auth_service.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.syncplatform.auth_service.dto.*;
import com.syncplatform.auth_service.service.AuthService;
import com.syncplatform.auth_service.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(Map.of("message", "Signup successful. Check logs for verification link."));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified. You can now log in."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) throws Exception {
        return ResponseEntity.ok(authService.login(request));
    }

    // JWK Set endpoint — other services call this to validate JWTs
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwtService.getPublicJwkSet().toJSONObject();
    }
}