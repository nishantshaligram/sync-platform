package com.syncplatform.sync_core_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ManualSyncQuotaService {

    private final StringRedisTemplate redisTemplate;

    private static final String CHECK_AND_INCREMENT_SCRIPT = """
            local count = redis.call('GET', KEYS[1])
            count = tonumber(count) or 0
            if count >= tonumber(ARGV[1]) then
                return 0
            end
            redis.call('INCR', KEYS[1])
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            return 1
            """;

    private static final String REFUND_SCRIPT = """
            local count = redis.call('GET', KEYS[1])
            if count and tonumber(count) > 0 then
                redis.call('DECR', KEYS[1])
            end
            return 1
            """;

    public boolean checkAndIncrement(UUID connectionId, int dailyLimit) {
        if (dailyLimit < 0) {
            return true; // unlimited
        }

        String key = quotaKey(connectionId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
                CHECK_AND_INCREMENT_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(dailyLimit),
                "172800" // 48 hours TTL
        );

        return Long.valueOf(1L).equals(result);
    }

    public void refund(UUID connectionId) {
        String key = quotaKey(connectionId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
                REFUND_SCRIPT, Long.class);

        redisTemplate.execute(script, List.of(key));
    }

    private String quotaKey(UUID connectionId) {
        return "manual_quota:" + connectionId + ":" + LocalDate.now();
    }
}