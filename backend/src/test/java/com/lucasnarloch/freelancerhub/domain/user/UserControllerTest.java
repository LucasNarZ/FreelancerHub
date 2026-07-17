package com.lucasnarloch.freelancerhub.domain.user;

import com.lucasnarloch.freelancerhub.domain.user.dtos.UserResponseDto;
import com.lucasnarloch.freelancerhub.domain.user.exceptions.UserNotFound;
import com.lucasnarloch.freelancerhub.infra.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsRequestsWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAuthenticatedUser() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id))
                .thenReturn(new UserResponseDto(id, "Test User", "user@example.com", true, false));

        mockMvc.perform(get("/users/me").with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.emailConfirmed").value(true));
    }

    @Test
    void returnsNotFoundWhenTokenUserNoLongerExists() throws Exception {
        UUID id = UUID.randomUUID();
        when(userService.findById(id)).thenThrow(new UserNotFound());

        mockMvc.perform(get("/users/me").with(jwt().jwt(token -> token.subject(id.toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));
    }
}
