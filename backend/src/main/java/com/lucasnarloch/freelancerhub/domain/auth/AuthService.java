package com.lucasnarloch.freelancerhub.domain.auth;

import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.LoginResponseDto;
import com.lucasnarloch.freelancerhub.domain.auth.dtos.RegisterUserDto;
import com.lucasnarloch.freelancerhub.domain.user.User;
import com.lucasnarloch.freelancerhub.domain.user.UserRepository;
import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponseDto login(LoginDto userCredentials) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(userCredentials.email(), userCredentials.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        var token = jwtService.generateAccessToken((User) auth.getPrincipal());

        return new LoginResponseDto(token);
    }

    public UserResponseDto register(RegisterUserDto body) {
        String passwordHash = passwordEncoder.encode(body.password());
        User user = new User(body.name(), body.email(), passwordHash);

        return UserResponseDto.from(userRepository.save(user));
    }
}
