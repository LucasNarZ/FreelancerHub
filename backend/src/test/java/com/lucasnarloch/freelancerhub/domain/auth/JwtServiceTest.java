package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.infra.config.JwtConfig;
import com.lucasnarloch.freelancerhub.infra.config.JwtProperties;
import com.lucasnarloch.freelancerhub.domain.auth.exceptions.InvalidRefreshToken;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void generatesSignedAccessTokenWithExpectedClaims() {
        var secretKey = new SecretKeySpec(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        var encoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var service = new JwtService(encoder, new JwtProperties("secret", Duration.ofMinutes(15), Duration.ofDays(30)), decoder);
        UUID userId = UUID.randomUUID();
        Instant beforeIssuance = Instant.now();

        var jwt = decoder.decode(service.generateAccessToken(userId));

        assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtConfig.ISSUER);
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("token_type")).isEqualTo("access");
        assertThat(jwt.getIssuedAt()).isBetween(beforeIssuance.minusSeconds(1), Instant.now().plusSeconds(1));
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(Duration.ofMinutes(15)));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256").containsEntry("typ", "JWT");
    }

    @Test
    void generatesRefreshTokenWithRefreshType() {
        var secretKey = new SecretKeySpec(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        var encoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var service = new JwtService(encoder, new JwtProperties("secret", Duration.ofMinutes(15), Duration.ofDays(30)), decoder);

        UUID userId = UUID.randomUUID();
        var jwt = service.decodeRefreshToken(service.generateRefreshToken(userId));
        var secondJwt = service.decodeRefreshToken(service.generateRefreshToken(userId));

        assertThat(jwt.getClaimAsString("token_type")).isEqualTo("refresh");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(UUID.fromString(jwt.getId())).isNotNull();
        assertThat(secondJwt.getId()).isNotEqualTo(jwt.getId());
    }

    @Test
    void rejectsAccessTokenAsRefreshToken() {
        var secretKey = new SecretKeySpec(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        var encoder = NimbusJwtEncoder.withSecretKey(secretKey).build();
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        var service = new JwtService(encoder, new JwtProperties("secret", Duration.ofMinutes(15), Duration.ofDays(30)), decoder);

        assertThatThrownBy(() -> service.decodeRefreshToken(service.generateAccessToken(UUID.randomUUID())))
                .isInstanceOf(InvalidRefreshToken.class)
                .hasMessage("Invalid refresh token");
    }
}
