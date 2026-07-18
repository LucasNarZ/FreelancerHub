package com.lucasnarloch.freelancerhub.domain.auth;

import java.time.Duration;

public interface RefreshTokenRepository {
    void save(String userId, String refreshToken, Duration expiration);

    boolean isValid(String userId, String refreshToken);

    void revoke(String userId, String refreshToken);
}
