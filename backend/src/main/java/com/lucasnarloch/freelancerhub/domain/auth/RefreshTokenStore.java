package com.lucasnarloch.freelancerhub.domain.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Service
public class RefreshTokenStore {
    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(String userId, String refreshToken, Duration expiration) {
        redis.opsForValue().set(key(userId, refreshToken), "active", expiration);
    }

    public boolean isValid(String userId, String refreshToken) {
        return Boolean.TRUE.equals(redis.hasKey(key(userId, refreshToken)));
    }

    public void revoke(String userId, String refreshToken) {
        redis.delete(key(userId, refreshToken));
    }

    private String key(String userId, String refreshToken) {
        return "refresh-token:%s:%s".formatted(userId, sha256(refreshToken));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
