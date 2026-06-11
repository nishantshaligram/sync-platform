package com.syncplatform.qbo_connector_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QboRateLimiterService {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_SCRIPT = """
        local key = KEYS[1]
        local capacity = tonumber(ARGV[1])
        local refill_rate = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        
        local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
        local tokens = tonumber(bucket[1]) or capacity
        local last_refill = tonumber(bucket[2]) or now
        
        local elapsed = now - last_refill
        local tokens_to_add = elapsed * refill_rate / 1000
        tokens = math.min(capacity, tokens + tokens_to_add)
        
        if tokens >= 1 then
            tokens = tokens - 1
            redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
            redis.call('EXPIRE', key, 60)
            return 1
        else
            redis.call('HMSET', key, 'tokens', tokens, 'last_refill', now)
            redis.call('EXPIRE', key, 60)
            return 0
        end
        """;

    public boolean tryAcquire(String realmId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(
            RATE_LIMIT_SCRIPT, Long.class);

        Long result = redisTemplate.execute(
            script,
            List.of("qbo_ratelimit:" + realmId),
            "8",    // capacity
            "8",    // refill rate per second
            String.valueOf(System.currentTimeMillis())
        );

        return Long.valueOf(1L).equals(result);
    }
}