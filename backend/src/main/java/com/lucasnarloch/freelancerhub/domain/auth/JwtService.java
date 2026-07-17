package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.user.User;
import com.lucasnarloch.freelancerhub.infra.config.JwtConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenExpiration;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${security.jwt.access-token-expiration}") Duration accessTokenExpiration
    ) {
        this.jwtEncoder = jwtEncoder;
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(User user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.ISSUER)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(accessTokenExpiration))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
