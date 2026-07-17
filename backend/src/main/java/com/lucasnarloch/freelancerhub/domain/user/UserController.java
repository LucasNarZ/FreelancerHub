package com.lucasnarloch.freelancerhub.domain.user;

import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponseDto findCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return userService.findById(UUID.fromString(jwt.getSubject()));
    }
}
