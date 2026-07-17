package com.lucasnarloch.freelancerhub.domain.user;

import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import com.lucasnarloch.freelancerhub.domain.user.exceptions.UserNotFound;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDto findById(UUID id) {
        User user = userRepository.findById(id).orElseThrow(UserNotFound::new);

        return UserResponseDto.from(user);
    }
}
