package com.syncplatform.qbo_connector_service.service;

import com.syncplatform.qbo_connector_service.document.CommandLog;
import com.syncplatform.qbo_connector_service.dto.QboCommand;
import com.syncplatform.qbo_connector_service.dto.QboCommandResult;
import com.syncplatform.qbo_connector_service.entity.PlatformAccount;
import com.syncplatform.qbo_connector_service.repository.CommandLogRepository;
import com.syncplatform.qbo_connector_service.repository.PlatformAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QboCommandConsumer {

    private final PlatformAccountRepository platformAccountRepository;
    private final CommandLogRepository commandLogRepository;
    private final QboRateLimiterService rateLimiterService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "qbo.commands",
                   groupId = "qbo-connector-group")
    public void consumeCommand(QboCommand command) {
        log.info("Received QBO command: {} for connection: {}",
            command.getCommandType(), command.getSyncConnectionId());

        String realmId = command.getSyncConnectionId();

        // Check rate limit
        if (!rateLimiterService.tryAcquire(realmId)) {
            log.warn("Rate limit hit for realm: {}, requeueing command: {}",
                realmId, command.getCommandId());
            // Re-publish with small delay — in production use delayed queue
            kafkaTemplate.send("qbo.commands", command.getSyncConnectionId(), command);
            return;
        }

        // Get access token
        PlatformAccount account = platformAccountRepository
            .findByExternalAccountId(realmId)
            .orElse(null);

        if (account == null) {
            log.error("No platform account found for realm: {}", realmId);
            publishResult(command, "failed", null, "No platform account found");
            return;
        }

        String accessToken = new String(
            account.getAccessTokenEncrypted(), StandardCharsets.UTF_8);

        // Check token expiry
        if (account.getTokenExpiresAt() != null
                && account.getTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            log.info("Token expired for realm: {}, refreshing...", realmId);
            try {
                String refreshToken = new String(
                    account.getRefreshTokenEncrypted(), StandardCharsets.UTF_8);
                // Token refresh would be called here via QboOAuthService
                // For MVP, just log and fail
                publishResult(command, "failed", null, "Token expired - reauthorization needed");
                return;
            } catch (Exception e) {
                publishResult(command, "failed", null, "Token refresh failed: " + e.getMessage());
                return;
            }
        }

        // Execute command
        long startTime = System.currentTimeMillis();
        try {
            String externalId = executeCommand(command, realmId, accessToken);
            long duration = System.currentTimeMillis() - startTime;

            // Log to MongoDB
            logCommand(command, null, 200, duration, "success");

            publishResult(command, "success", externalId, null);
            log.info("Command {} executed successfully", command.getCommandId());

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Command {} failed: {}", command.getCommandId(), e.getMessage());
            logCommand(command, e.getMessage(), 500, duration, "failed");
            publishResult(command, "failed", null, e.getMessage());
        }
    }

    private String executeCommand(QboCommand command,
                                   String realmId,
                                   String accessToken) {
        // MVP: log the command — real QBO API calls implemented with sandbox
        log.info("Executing QBO command: {} type: {} for realm: {}",
            command.getCommandId(), command.getCommandType(), realmId);

        // Return a mock external ID for MVP demo
        // Real implementation calls QBO API via RestTemplate
        return "qbo-" + command.getCanonicalEntityId();
    }

    private void publishResult(QboCommand command,
                                String status,
                                String externalId,
                                String errorMessage) {
        QboCommandResult result = new QboCommandResult();
        result.setCommandId(command.getCommandId());
        result.setSyncRunId(command.getSyncRunId());
        result.setSyncConnectionId(command.getSyncConnectionId());
        result.setCanonicalEntityId(command.getCanonicalEntityId());
        result.setCanonicalEntityType(command.getCanonicalEntityType());
        result.setExternalId(externalId);
        result.setStatus(status);
        result.setErrorMessage(errorMessage);
        result.setCompletedAt(Instant.now().toString());

        kafkaTemplate.send("qbo.command.results",
            command.getSyncConnectionId(), result);
    }

    private void logCommand(QboCommand command,
                             String error,
                             int httpStatus,
                             long durationMs,
                             String status) {
        CommandLog log = CommandLog.builder()
            .syncConnectionId(command.getSyncConnectionId())
            .syncRunId(command.getSyncRunId())
            .commandType(command.getCommandType())
            .attempt(1)
            .httpStatus(httpStatus)
            .durationMs(durationMs)
            .status(status)
            .occurredAt(Instant.now())
            .build();

        commandLogRepository.save(log);
    }
}