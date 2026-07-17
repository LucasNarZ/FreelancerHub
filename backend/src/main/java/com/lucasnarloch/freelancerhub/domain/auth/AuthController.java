package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginResponseDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.RegisterUserDto;
import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginDto body) {
        return authService.login(body);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@Valid @RequestBody RegisterUserDto body) {
        return authService.register(body);
    }
}
