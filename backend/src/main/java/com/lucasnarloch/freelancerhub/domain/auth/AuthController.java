package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginResponseDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.RegisterUserDto;
import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final Duration refreshTokenExpiration;

    public AuthController(
            AuthService authService,
            @Value("${security.jwt.refresh-token-expiration}") Duration refreshTokenExpiration
    ) {
        this.authService = authService;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginDto body, jakarta.servlet.http.HttpServletResponse response) {
        TokenPair tokens = authService.login(body);
        setRefreshCookie(response, tokens.refreshToken());
        return new LoginResponseDto(tokens.accessToken());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@Valid @RequestBody RegisterUserDto body) {
        return authService.register(body);
    }

    @PostMapping("/refresh")
    public LoginResponseDto refresh(
            @CookieValue("refresh_token") String refreshToken,
            HttpServletResponse response
    ) {
        TokenPair tokens = authService.refresh(refreshToken);
        setRefreshCookie(response, tokens.refreshToken());
        return new LoginResponseDto(tokens.accessToken());
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(refreshTokenExpiration)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
