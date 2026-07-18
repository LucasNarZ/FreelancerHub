package com.lucasnarloch.freelancerhub.infra.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisRefreshTokenStoreTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final RedisRefreshTokenStore store = new RedisRefreshTokenStore(redis);

    @Test
    void storesTokenHashWithItsExpiration() {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);

        store.save("user-123", "refresh-token", Duration.ofDays(30));

        verify(values).set(key("user-123", "refresh-token"), "active", Duration.ofDays(30));
    }

    @Test
    void considersStoredTokenValid() {
        when(redis.hasKey(key("user-123", "refresh-token"))).thenReturn(true);

        assertThat(store.isValid("user-123", "refresh-token")).isTrue();
    }

    @Test
    void revokesTokenByDeletingItsHash() {
        store.revoke("user-123", "refresh-token");

        verify(redis).delete(key("user-123", "refresh-token"));
    }

    private String key(String userId, String token) {
        return "refresh-token:%s:%s".formatted(userId, sha256(token));
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
