package br.com.foody.tracking;

import br.com.foody.tracking.modules.auth.entity.User;
import br.com.foody.tracking.modules.auth.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Same configuration as OrderFlowIntegrationTest, so the cached context (and its own in-memory
// database) is reused instead of booting a second one. E-mails are unique per run.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
        properties =
                "spring.datasource.url=jdbc:sqlite:file:flowdb?mode=memory&cache=shared&foreign_keys=true")
class PasswordResetIntegrationTest {

    private static final String OLD_PASSWORD = "senha-forte-123";
    private static final String NEW_PASSWORD = "nova-senha-456";

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    @Value("${app.security.admin.email}")
    private String adminEmail;

    @Value("${app.security.admin.password}")
    private String adminPassword;

    @Test
    void shouldLoginOnlyWithNewPasswordAndKeepRole_whenUserResetsPassword() throws Exception {
        // Given
        String email = "carla-" + UUID.randomUUID().toString().substring(0, 8) + "@exemplo.com";
        register(email).andExpect(status().isCreated());

        // When the reset carries a role and a stale bearer, both ignored
        mockMvc.perform(
                        post("/api/v1/auth/password-reset")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"email": "%s", "newPassword": "%s", "role": "ADMIN"}
                    """
                                                .formatted(email.toUpperCase(), NEW_PASSWORD)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Then
        login(email, OLD_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
        String token = read(login(email, NEW_PASSWORD).andExpect(status().isOk()), "$.accessToken");
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldAnswerNoContent_whenEmailIsUnknown() throws Exception {
        // When / Then
        reset("ninguem-" + UUID.randomUUID() + "@exemplo.com")
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldAnswerNoContentAndKeepHash_whenEmailBelongsToAdmin() throws Exception {
        // Given
        String hashBefore =
                userRepository.findByEmail(adminEmail).map(User::getPasswordHash).orElseThrow();

        // When
        reset(adminEmail).andExpect(status().isNoContent()).andExpect(content().string(""));

        // Then
        assertThat(userRepository.findByEmail(adminEmail).map(User::getPasswordHash))
                .contains(hashBefore);
        login(adminEmail, adminPassword).andExpect(status().isOk());
        login(adminEmail, NEW_PASSWORD).andExpect(status().isUnauthorized());
    }

    private ResultActions register(String email) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                {"name": "Carla Dias", "email": "%s", "password": "%s"}
                """
                                        .formatted(email, OLD_PASSWORD)));
    }

    private ResultActions reset(String email) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                {"email": "%s", "newPassword": "%s"}
                """
                                        .formatted(email, NEW_PASSWORD)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                {"email": "%s", "password": "%s"}
                """
                                        .formatted(email, password)));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }
}
