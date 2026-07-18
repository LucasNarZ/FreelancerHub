package com.lucasnarloch.freelancerhub.infra.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void rejectsSecretsShorterThan256Bits() {
        String encodedSecret = Base64.getEncoder()
                .encodeToString("short-secret".getBytes(StandardCharsets.UTF_8));

        JwtProperties jwtProperties = new JwtProperties(encodedSecret, Duration.ofMinutes(15), Duration.ofDays(30));

        assertThatThrownBy(() -> jwtConfig.jwtSecretKey(jwtProperties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT_SECRET must contain at least 256 bits");
    }
}
