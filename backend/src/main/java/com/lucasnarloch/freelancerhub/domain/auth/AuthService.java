package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.RegisterUserDto;
import com.lucasnarloch.freelancerhub.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.freelancerhub.domain.user.User;
import com.lucasnarloch.freelancerhub.domain.user.UserRepository;
import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import com.lucasnarloch.freelancerhub.domain.user.exceptions.EmailAlreadyRegistered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final Duration refreshTokenExpiration;

    public AuthService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordEncoder passwordEncoder, RefreshTokenStore refreshTokenStore,
            @Value("${security.jwt.refresh-token-expiration}") Duration refreshTokenExpiration
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenStore = refreshTokenStore;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public TokenPair login(LoginDto userCredentials) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(userCredentials.email(), userCredentials.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        User user = (User) auth.getPrincipal();

        var accessToken = jwtService.generateAccessToken(user.getId());
        var refreshToken = jwtService.generateRefreshToken(user.getId());
        refreshTokenStore.save(user.getId().toString(), refreshToken, refreshTokenExpiration);

        return new TokenPair(accessToken, refreshToken);
    }

    public UserResponseDto register(RegisterUserDto body) {
        if (userRepository.findByEmail(body.email()).isPresent()) {
            throw new EmailAlreadyRegistered();
        }

        String passwordHash = passwordEncoder.encode(body.password());
        User user = new User(body.name(), body.email(), passwordHash);

        try {
            return UserResponseDto.from(userRepository.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegistered();
        }
    }

    public TokenPair refresh(String refreshToken) {
        var refreshJwt = jwtService.decodeRefreshToken(refreshToken);
        String userId = refreshJwt.getSubject();

        if (!refreshTokenStore.isValid(userId.toString(), refreshToken)) {
            throw new InvalidRefreshToken();
        }

        refreshTokenStore.revoke(userId.toString(), refreshToken);
        String newRefreshToken = jwtService.generateRefreshToken(java.util.UUID.fromString(userId));
        refreshTokenStore.save(userId, newRefreshToken, refreshTokenExpiration);

        String accessToken = jwtService.generateAccessToken(java.util.UUID.fromString(userId));

        return new TokenPair(accessToken, newRefreshToken);
    }


}
