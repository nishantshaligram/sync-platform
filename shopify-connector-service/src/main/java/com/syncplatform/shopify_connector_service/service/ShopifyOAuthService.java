package com.syncplatform.shopify_connector_service.service;

import com.syncplatform.shopify_connector_service.entity.PlatformAccount;
import com.syncplatform.shopify_connector_service.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopifyOAuthService {

    private final PlatformAccountRepository platformAccountRepository;
    private final RestTemplate restTemplate;

    @Value("${shopify.client-id}")
    private String clientId;

    @Value("${shopify.client-secret}")
    private String clientSecret;

    @Value("${shopify.redirect-uri}")
    private String redirectUri;

    @Value("${shopify.scopes}")
    private String scopes;

    public String buildAuthorizationUrl(String shopDomain, String state) {
        return String.format(
                "https://%s/admin/oauth/authorize?client_id=%s&scope=%s&redirect_uri=%s&state=%s",
                shopDomain, clientId, scopes, redirectUri, state);
    }

    @Transactional
    public PlatformAccount handleCallback(UUID userId, String shopDomain,
            String code, String state) {
        // Exchange code for access token
        String tokenUrl = String.format("https://%s/admin/oauth/access_token", shopDomain);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Failed to exchange Shopify OAuth code for token");
        }

        String accessToken = (String) response.getBody().get("access_token");

        // Verify token by fetching shop info
        String shopName = verifyToken(shopDomain, accessToken);

        // Encrypt token (simple Base64 for MVP — use proper encryption in prod)
        byte[] encryptedToken = accessToken.getBytes(StandardCharsets.UTF_8);

        // Save or update platform account
        PlatformAccount account = platformAccountRepository
                .findByUserIdAndPlatformAndExternalAccountId(userId, "shopify", shopDomain)
                .orElse(PlatformAccount.builder()
                        .userId(userId)
                        .platform("shopify")
                        .platformKind("ecommerce")
                        .externalAccountId(shopDomain)
                        .build());

        account.setDisplayName(shopName);
        account.setAccessTokenEncrypted(encryptedToken);
        account.setStatus("connected");
        account.setScopes(scopes.split(","));

        return platformAccountRepository.save(account);
    }

    private String verifyToken(String shopDomain, String accessToken) {
        String shopUrl = String.format("https://%s/admin/api/2024-01/shop.json", shopDomain);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Shopify-Access-Token", accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                shopUrl, HttpMethod.GET, request, Map.class);

        if (response.getBody() != null && response.getBody().containsKey("shop")) {
            Map<String, Object> shop = (Map<String, Object>) response.getBody().get("shop");
            return (String) shop.getOrDefault("name", shopDomain);
        }
        return shopDomain;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}