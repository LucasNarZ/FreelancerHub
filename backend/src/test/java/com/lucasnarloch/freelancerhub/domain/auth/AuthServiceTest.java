package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.RegisterUserDto;
import com.lucasnarloch.freelancerhub.domain.user.User;
import com.lucasnarloch.freelancerhub.domain.user.UserRepository;
import com.lucasnarloch.freelancerhub.domain.user.exceptions.EmailAlreadyRegistered;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

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
        User user = new User("Test User", "user@example.com", "password-hash");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("signed-token");

        var response = authService.login(request);

        var authenticationCaptor = org.mockito.ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getValue().getPrincipal()).isEqualTo("user@example.com");
        assertThat(authenticationCaptor.getValue().getCredentials()).isEqualTo("password123");
        assertThat(response.token()).isEqualTo("signed-token");
    }
}
