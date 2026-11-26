package org.example.adapter.out.persistent;

import org.example.application.security.RefreshTokenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenAdapterTest {

    private static final Instant NOW = Instant.parse("2026-11-25T08:00:00Z");
    private static final String KEY = RedisRefreshTokenAdapter.KEY_PREFIX + "token-1";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisRefreshTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisRefreshTokenAdapter(
                redisTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void savesWhitelistEntryWithRemainingLifetime() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.save(new RefreshTokenSession(
                "token-1",
                42L,
                NOW.plus(Duration.ofDays(7))
        ));

        verify(valueOperations).set(KEY, "42", Duration.ofDays(7));
    }

    @Test
    void claimsTokenByAtomicallyDeletingWhitelistEntry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(KEY)).thenReturn("42");

        assertTrue(adapter.claim("token-1", NOW));
    }

    @Test
    void missingWhitelistEntryCannotBeClaimed() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(KEY)).thenReturn(null);

        assertFalse(adapter.claim("token-1", NOW));
    }

    @Test
    void revokeDeletesWhitelistEntry() {
        adapter.revoke("token-1", NOW);

        verify(redisTemplate).delete(KEY);
    }

    @Test
    void expiredSessionIsNotWritten() {
        assertThrows(
                IllegalArgumentException.class,
                () -> adapter.save(new RefreshTokenSession("token-1", 42L, NOW))
        );
    }
}
