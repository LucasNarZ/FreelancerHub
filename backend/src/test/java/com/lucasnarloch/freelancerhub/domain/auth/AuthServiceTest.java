package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.RegisterUserDto;
import com.lucasnarloch.freelancerhub.domain.auth.exceptions.InvalidRefreshToken;
import com.lucasnarloch.freelancerhub.domain.user.User;
import com.lucasnarloch.freelancerhub.domain.user.UserRepository;
import com.lucasnarloch.freelancerhub.domain.user.exceptions.EmailAlreadyRegistered;
import com.lucasnarloch.freelancerhub.infra.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenRepository refreshTokenStore;

    @Mock
    private Authentication authentication;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                authenticationManager,
                jwtService,
                passwordEncoder,
                refreshTokenStore,
                new JwtProperties("secret", Duration.ofMinutes(15), Duration.ofDays(30))
        );
    }

    @Test
    void registersUserWithEncodedPassword() {
        RegisterUserDto request = new RegisterUserDto("Test User", "user@example.com", "password123");
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(response.name()).isEqualTo("Test User");
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsAlreadyRegisteredEmail() {
        RegisterUserDto request = new RegisterUserDto("Test User", "user@example.com", "password123");
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(java.util.Optional.of(new User("Existing", "user@example.com", "hash")));

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegistered.class)
                .hasMessage("Email already registered");
    }

    @Test
    void translatesConcurrentEmailConflict() {
        RegisterUserDto request = new RegisterUserDto("Test User", "user@example.com", "password123");
        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenThrow(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyRegistered.class)
                .hasMessage("Email already registered");
    }

    @Test
    void generatesTokenForAuthenticatedPrincipal() {
        LoginDto request = new LoginDto("user@example.com", "password123");
        UUID userId = UUID.randomUUID();
        User user = org.mockito.Mockito.mock(User.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(jwtService.generateAccessToken(userId)).thenReturn("signed-access-token");
        when(jwtService.generateRefreshToken(userId)).thenReturn("signed-refresh-token");

        var response = authService.login(request);

        var authenticationCaptor = org.mockito.ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getValue().getPrincipal()).isEqualTo("user@example.com");
        assertThat(authenticationCaptor.getValue().getCredentials()).isEqualTo("password123");
        assertThat(response.accessToken()).isEqualTo("signed-access-token");
        assertThat(response.refreshToken()).isEqualTo("signed-refresh-token");
        verify(refreshTokenStore).save(userId.toString(), "signed-refresh-token", Duration.ofDays(30));
    }

    @Test
    void rotatesValidRefreshToken() {
        UUID userId = UUID.randomUUID();
        Jwt refreshJwt = new Jwt(
                "signed-refresh-token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"),
                Map.of("sub", userId.toString(), "token_type", "refresh")
        );
        when(jwtService.decodeRefreshToken("signed-refresh-token")).thenReturn(refreshJwt);
        when(refreshTokenStore.isValid(userId.toString(), "signed-refresh-token")).thenReturn(true);
        when(jwtService.generateAccessToken(userId)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(userId)).thenReturn("new-refresh-token");

        var response = authService.refresh("signed-refresh-token");

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenStore).revoke(userId.toString(), "signed-refresh-token");
        verify(refreshTokenStore).save(userId.toString(), "new-refresh-token", Duration.ofDays(30));
    }

    @Test
    void rejectsRefreshTokenMissingFromRedis() {
        UUID userId = UUID.randomUUID();
        Jwt refreshJwt = new Jwt(
                "signed-refresh-token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "HS256"),
                Map.of("sub", userId.toString(), "token_type", "refresh")
        );
        when(jwtService.decodeRefreshToken("signed-refresh-token")).thenReturn(refreshJwt);

        assertThatThrownBy(() -> authService.refresh("signed-refresh-token"))
                .isInstanceOf(InvalidRefreshToken.class)
                .hasMessage("Invalid refresh token");
    }
}
