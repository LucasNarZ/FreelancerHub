package com.lucasnarloch.freelancerhub;

import com.jayway.jsonpath.JsonPath;
import com.lucasnarloch.freelancerhub.domain.auth.RefreshTokenRepository;
import com.lucasnarloch.freelancerhub.domain.user.User;
import com.lucasnarloch.freelancerhub.domain.user.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
@AutoConfigureMockMvc
class BackendIntegrationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:16")
    );

    @Container
    @ServiceConnection
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")
    ).withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void appliesFlywayMigrationsAndValidatesJpaMappings() {
        Integer appliedMigrations = jdbcClient.sql("""
                        SELECT COUNT(*)
                        FROM flyway_schema_history
                        WHERE success = true
                        """)
                .query(Integer.class)
                .single();

        assertThat(appliedMigrations).isEqualTo(1);
    }

    @Test
    void enforcesUniqueEmailAtDatabaseBoundary() {
        userRepository.saveAndFlush(new User("First User", "user@example.com", "hash"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                new User("Second User", "user@example.com", "hash")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void registersAndPersistsUserWithHashedPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("user@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        User persistedUser = userRepository.findByEmail("user@example.com").orElseThrow();
        assertThat(persistedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", persistedUser.getPassword())).isTrue();
    }

    @Test
    void rejectsInvalidRegistrationPayload() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void rejectsDuplicateRegistrationAsConflict() throws Exception {
        register("user@example.com");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("user@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    void logsInAndUsesAccessTokenToReadCurrentUser() throws Exception {
        register("user@example.com");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn();
        String response = loginResult.getResponse().getContentAsString();
        String token = JsonPath.read(response, "$.accessToken");
        User persistedUser = userRepository.findByEmail("user@example.com").orElseThrow();
        var jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo(persistedUser.getId().toString());
        String refreshToken = refreshTokenFrom(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE));

        assertThat(refreshTokenRepository.isValid(persistedUser.getId().toString(), refreshToken)).isTrue();

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(persistedUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void logsOutAndRevokesRefreshToken() throws Exception {
        register("user@example.com");
        User persistedUser = userRepository.findByEmail("user@example.com").orElseThrow();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String refreshToken = refreshTokenFrom(loginResult.getResponse().getHeader(HttpHeaders.SET_COOKIE));

        MvcResult logoutResult = mockMvc.perform(post("/auth/logout")
                        .cookie(new Cookie("refresh_token", refreshToken)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(logoutResult.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("refresh_token=", "Path=/auth", "Max-Age=0");
        assertThat(refreshTokenRepository.isValid(persistedUser.getId().toString(), refreshToken)).isFalse();
    }

    @Test
    void rejectsInvalidCredentials() throws Exception {
        register("user@example.com");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectsCurrentUserEndpoint() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson(email)))
                .andExpect(status().isCreated());
    }

    private String registrationJson(String email) {
        return """
                {
                  "name": "Test User",
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email);
    }

    private String refreshTokenFrom(String setCookieHeader) {
        return setCookieHeader.substring("refresh_token=".length(), setCookieHeader.indexOf(';'));
    }
}
