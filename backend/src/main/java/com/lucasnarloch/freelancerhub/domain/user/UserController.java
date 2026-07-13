package com.lucasnarloch.freelancerhub.domain.user;

import com.lucasnarloch.freelancerhub.domain.user.dtos.CreateUserRequestDto;
import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto save(@Valid @RequestBody CreateUserRequestDto body) {
        return userService.registerUser(body);
    }

    @GetMapping("/{id}")
    public UserResponseDto findById(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @GetMapping
    public List<UserResponseDto> findAll() {
        return userService.findAll();
    }


}
