package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.freelancerhub.infra.config.JwtConfig;
import com.lucasnarloch.freelancerhub.infra.config.JwtProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final JwtDecoder jwtDecoder;

    public JwtService(
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties,
            JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.jwtDecoder = jwtDecoder;
    }

    public String generateAccessToken(UUID userId) {
        return generateToken(userId, jwtProperties.accessTokenExpiration(), TokenType.ACCESS);
    }

    public String generateRefreshToken(UUID userId) {
        return generateToken(userId, jwtProperties.refreshTokenExpiration(), TokenType.REFRESH);
    }

    private String generateToken(UUID userId, Duration expirationTime, TokenType tokenType) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(expirationTime))
                .subject(userId.toString())
                .claim("token_type", tokenType.value())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Jwt decodeRefreshToken(String refreshToken) {
        try {
            Jwt jwt = jwtDecoder.decode(refreshToken);
            if (!TokenType.REFRESH.value().equals(jwt.getClaimAsString("token_type"))) {
                throw new InvalidRefreshToken();
            }
            UUID.fromString(jwt.getSubject());
            return jwt;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidRefreshToken();
        }
    }
}
