package org.example.adapter.out.persistent;

import lombok.RequiredArgsConstructor;
import org.example.application.security.RefreshTokenSession;
import org.example.application.security.RefreshTokenStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenAdapter implements RefreshTokenStore {

    static final String KEY_PREFIX = "mini-go:auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    @Override
    public void save(RefreshTokenSession session) {
        Duration ttl = Duration.between(clock.instant(), session.expiresAt());
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("RefreshToken 已过期，无法写入 Redis");
        }
        redisTemplate.opsForValue().set(
                key(session.tokenId()),
                String.valueOf(session.principalId()),
                ttl
        );
    }

    @Override
    public boolean claim(String tokenId, Instant claimedAt) {
        return redisTemplate.opsForValue().getAndDelete(key(tokenId)) != null;
    }

    @Override
    public void revoke(String tokenId, Instant revokedAt) {
        redisTemplate.delete(key(tokenId));
    }

    private String key(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("RefreshToken ID 不能为空");
        }
        return KEY_PREFIX + tokenId;
    }
}
