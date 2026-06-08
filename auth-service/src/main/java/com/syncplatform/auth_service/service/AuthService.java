package com.syncplatform.auth_service.service;

import com.syncplatform.auth_service.dto.*;
import com.syncplatform.auth_service.entity.User;
import com.syncplatform.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already in use");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .status(User.UserStatus.pending_verification)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .build();

        userRepository.save(user);

        // MVP: log the link instead of sending email
        log.info("=== VERIFY EMAIL ===");
        log.info("Verification link: http://localhost:8080/auth/verify?token={}", verificationToken);
        log.info("====================");
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired verification token"));

        user.setStatus(User.UserStatus.active);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest request) throws Exception {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.getStatus() != User.UserStatus.active) {
            throw new IllegalArgumentException("Please verify your email before logging in");
        }

        String token = jwtService.generateToken(user.getId().toString(), user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getId().toString());
    }
}