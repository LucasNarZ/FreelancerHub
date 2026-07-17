package com.lucasnarloch.freelancerhub.domain.user;

import com.lucasnarloch.freelancerhub.domain.user.exceptions.UserNotFound;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void returnsUserById() {
        UUID id = UUID.randomUUID();
        User user = new User("Test User", "user@example.com", "password-hash");
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        var response = userService.findById(id);

        assertThat(response.name()).isEqualTo("Test User");
        assertThat(response.email()).isEqualTo("user@example.com");
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(id))
                .isInstanceOf(UserNotFound.class)
                .hasMessage("User not found");
    }
}
