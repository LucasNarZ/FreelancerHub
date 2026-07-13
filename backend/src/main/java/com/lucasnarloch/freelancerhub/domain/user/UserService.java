package com.lucasnarloch.freelancerhub.domain.user;

import com.lucasnarloch.freelancerhub.domain.user.dtos.CreateUserRequestDto;
import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import com.lucasnarloch.freelancerhub.domain.user.exceptions.UserNotFound;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponseDto registerUser(CreateUserRequestDto body) {
        String passwordHash = passwordEncoder.encode(body.password());

        User user = new User(body.name(), body.email(), passwordHash);

        return UserResponseDto.from(userRepository.save(user));
    }

    public UserResponseDto findById(UUID id) {
        User user = userRepository.findById(id).orElseThrow(UserNotFound::new);

        return UserResponseDto.from(user);
    }

    public List<UserResponseDto> findAll() {
        List<User> users = userRepository.findAll();

        return users.stream().map(UserResponseDto::from).toList();
    }
}
