package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.RegisterUserDto;
import com.lucasnarloch.freelancerhub.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.freelancerhub.domain.user.User;
import com.lucasnarloch.freelancerhub.domain.user.UserRepository;
import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import com.lucasnarloch.freelancerhub.domain.user.exceptions.EmailAlreadyRegistered;
import com.lucasnarloch.freelancerhub.infra.config.JwtProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public AuthService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public TokenPair login(LoginDto userCredentials) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(userCredentials.email(), userCredentials.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        User user = (User) auth.getPrincipal();

        var accessToken = jwtService.generateAccessToken(user.getId());
        var refreshToken = jwtService.generateRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId().toString(), refreshToken, jwtProperties.refreshTokenExpiration());

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

        if (!refreshTokenRepository.isValid(userId, refreshToken)) {
            throw new InvalidRefreshToken();
        }

        refreshTokenRepository.revoke(userId, refreshToken);
        String newRefreshToken = jwtService.generateRefreshToken(java.util.UUID.fromString(userId));
        refreshTokenRepository.save(userId, newRefreshToken, jwtProperties.refreshTokenExpiration());

        String accessToken = jwtService.generateAccessToken(java.util.UUID.fromString(userId));

        return new TokenPair(accessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        var refreshJwt = jwtService.decodeRefreshToken(refreshToken);
        String userId = refreshJwt.getSubject();

        refreshTokenRepository.revoke(userId, refreshToken);
    }

}
