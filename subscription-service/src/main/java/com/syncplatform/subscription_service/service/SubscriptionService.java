package com.syncplatform.subscription_service.service;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.syncplatform.subscription_service.dto.*;
import com.syncplatform.subscription_service.entity.*;
import com.syncplatform.subscription_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    public String createCheckoutSession(UUID userId, CheckoutRequest request) throws Exception {
        Stripe.apiKey = stripeApiKey;

        Plan plan = planRepository.findByCode(request.getPlanCode())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + request.getPlanCode()));

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPrice(plan.getStripePriceId())
                        .setQuantity(1L)
                        .build()
                )
                .setSuccessUrl(request.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(request.getCancelUrl())
                .putMetadata("user_id", userId.toString())
                .putMetadata("plan_code", plan.getCode())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public void handleSubscriptionCreated(String stripeSubscriptionId,
                                           String stripePriceId,
                                           String stripeCustomerId,
                                           String userId,
                                           long periodStart,
                                           long periodEnd) {
        Plan plan = planRepository.findAll().stream()
                .filter(p -> stripePriceId.equals(p.getStripePriceId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No plan for price: " + stripePriceId));

        UUID userUuid = UUID.fromString(userId);

        Subscription subscription = subscriptionRepository
                .findByUserId(userUuid)
                .orElse(Subscription.builder().userId(userUuid).build());

        subscription.setPlan(plan);
        subscription.setStripeCustomerId(stripeCustomerId);
        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setStatus(Subscription.SubscriptionStatus.active);
        subscription.setCurrentPeriodStart(
                java.time.Instant.ofEpochSecond(periodStart)
                        .atOffset(java.time.ZoneOffset.UTC));
        subscription.setCurrentPeriodEnd(
                java.time.Instant.ofEpochSecond(periodEnd)
                        .atOffset(java.time.ZoneOffset.UTC));

        subscriptionRepository.save(subscription);

        // Invalidate plan limits cache
        redisTemplate.delete("plan:" + userId);
        log.info("Subscription activated for user {} on plan {}", userId, plan.getCode());
    }

    public PlanLimitsResponse getPlanLimits(UUID userId) {
        String cacheKey = "plan:" + userId;
        // Try cache first — for MVP just query DB directly
        // Redis caching can be layered on top later
        Subscription sub = subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No subscription for user"));

        Plan plan = sub.getPlan();
        return new PlanLimitsResponse(
                plan.getCode(),
                plan.getMaxConnections(),
                plan.getAllowedIntervals(),
                plan.getManualSyncPerDay(),
                plan.getBackfillDays(),
                plan.getHistoryRetentionDays()
        );
    }

    @Transactional
    public void createDemoSubscription(UUID userId) {
        Plan demoPlan = planRepository.findByCode("demo")
                .orElseThrow(() -> new IllegalStateException("Demo plan not seeded"));

        Subscription subscription = Subscription.builder()
                .userId(userId)
                .plan(demoPlan)
                .status(Subscription.SubscriptionStatus.demo)
                .cancelAtPeriodEnd(false)
                .build();

        subscriptionRepository.save(subscription);
    }
}