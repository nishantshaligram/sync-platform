package com.syncplatform.qbo_connector_service.service;

import com.syncplatform.qbo_connector_service.entity.PlatformAccount;
import com.syncplatform.qbo_connector_service.entity.QboOAuthState;
import com.syncplatform.qbo_connector_service.repository.PlatformAccountRepository;
import com.syncplatform.qbo_connector_service.repository.QboOAuthStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QboOAuthService {

    private final PlatformAccountRepository platformAccountRepository;
    private final QboOAuthStateRepository oAuthStateRepository;
    private final RestTemplate restTemplate;

    @Value("${qbo.client-id}")
    private String clientId;

    @Value("${qbo.client-secret}")
    private String clientSecret;

    @Value("${qbo.redirect-uri}")
    private String redirectUri;

    @Value("${qbo.environment}")
    private String environment;

    public String buildAuthorizationUrl(UUID userId) {
        String state = UUID.randomUUID().toString();

        // Save state for validation on callback
        oAuthStateRepository.save(QboOAuthState.builder()
            .userId(userId)
            .state(state)
            .createdAt(OffsetDateTime.now())
            .expiresAt(OffsetDateTime.now().plusMinutes(10))
            .build());

        return String.format(
            "https://appcenter.intuit.com/connect/oauth2" +
            "?client_id=%s&redirect_uri=%s&response_type=code" +
            "&scope=com.intuit.quickbooks.accounting&state=%s",
            clientId, redirectUri, state
        );
    }

    @Transactional
    public PlatformAccount handleCallback(String code, String state, String realmId) {
        // Validate state
        QboOAuthState oAuthState = oAuthStateRepository.findByState(state)
            .orElseThrow(() -> new IllegalArgumentException("Invalid OAuth state"));

        if (oAuthState.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("OAuth state expired");
        }

        UUID userId = oAuthState.getUserId();

        // Exchange code for tokens
        String tokenUrl = "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer";

        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            tokenUrl, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to exchange QBO OAuth code");
        }

        Map<String, Object> tokens = response.getBody();
        String accessToken = (String) tokens.get("access_token");
        String refreshToken = (String) tokens.get("refresh_token");
        Integer expiresIn = (Integer) tokens.get("expires_in");

        // Fetch company info to get display name
        String companyName = fetchCompanyName(realmId, accessToken);

        // Save platform account
        PlatformAccount account = platformAccountRepository
            .findByUserIdAndPlatformAndExternalAccountId(userId, "qbo", realmId)
            .orElse(PlatformAccount.builder()
                .userId(userId)
                .platform("qbo")
                .platformKind("accounting")
                .externalAccountId(realmId)
                .build());

        account.setDisplayName(companyName);
        account.setAccessTokenEncrypted(
            accessToken.getBytes(StandardCharsets.UTF_8));
        account.setRefreshTokenEncrypted(
            refreshToken.getBytes(StandardCharsets.UTF_8));
        account.setTokenExpiresAt(
            OffsetDateTime.now().plusSeconds(expiresIn));
        account.setStatus("connected");

        // Clean up state
        oAuthStateRepository.delete(oAuthState);

        return platformAccountRepository.save(account);
    }

    public String refreshAccessToken(String realmId, String refreshToken) {
        String tokenUrl = "https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer";

        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + credentials);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
            tokenUrl, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to refresh QBO token for realm: " + realmId);
        }

        String newAccessToken = (String) response.getBody().get("access_token");

        // Update stored token
        platformAccountRepository.findByExternalAccountId(realmId)
            .ifPresent(account -> {
                account.setAccessTokenEncrypted(
                    newAccessToken.getBytes(StandardCharsets.UTF_8));
                Integer expiresIn = (Integer) response.getBody().get("expires_in");
                account.setTokenExpiresAt(
                    OffsetDateTime.now().plusSeconds(expiresIn));
                platformAccountRepository.save(account);
            });

        return newAccessToken;
    }

    private String fetchCompanyName(String realmId, String accessToken) {
        try {
            String baseUrl = environment.equals("sandbox")
                ? "https://sandbox-quickbooks.api.intuit.com"
                : "https://quickbooks.api.intuit.com";

            String url = String.format(
                "%s/v3/company/%s/companyinfo/%s", baseUrl, realmId, realmId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            headers.set("Accept", "application/json");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, request, Map.class);

            if (response.getBody() != null
                    && response.getBody().containsKey("CompanyInfo")) {
                Map<String, Object> info =
                    (Map<String, Object>) response.getBody().get("CompanyInfo");
                return (String) info.getOrDefault("CompanyName", realmId);
            }
        } catch (Exception e) {
            log.warn("Could not fetch QBO company name: {}", e.getMessage());
        }
        return realmId;
    }
}