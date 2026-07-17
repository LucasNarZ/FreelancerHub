package com.lucasnarloch.freelancerhub.infra.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void rejectsSecretsShorterThan256Bits() {
        String encodedSecret = Base64.getEncoder()
                .encodeToString("short-secret".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> jwtConfig.jwtSecretKey(encodedSecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JWT_SECRET must contain at least 256 bits");
    }
}
