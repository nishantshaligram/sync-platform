package com.syncplatform.sync_core_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SyncLockService {

    private final StringRedisTemplate redisTemplate;

    public boolean isLocked(UUID connectionId) {
        return redisTemplate.hasKey("sync_lock:" + connectionId);
    }
}