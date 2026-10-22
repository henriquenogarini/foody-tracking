package br.com.foody.tracking.modules.auth.controller;

import br.com.foody.tracking.config.ClockConfig;
import br.com.foody.tracking.config.JacksonConfig;
import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.config.SecurityConfig;
import br.com.foody.tracking.modules.auth.dto.PasswordResetRequestDTO;
import br.com.foody.tracking.modules.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PasswordResetController.class)
@Import({SecurityConfig.class, JwtConfig.class, ClockConfig.class, JacksonConfig.class})
@ActiveProfiles("test")
class PasswordResetControllerTest {

    private static final String PATH = "/api/v1/auth/password-reset";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;

    @Test
    void shouldReturnNoContentAndPassStrippedEmail_whenRequestIsValid() throws Exception {
        // When
        mockMvc.perform(
                        post(PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"email": "  Ana@Exemplo.com ", "newPassword": "nova-senha-456"}
                    """))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Then
        ArgumentCaptor<PasswordResetRequestDTO> request =
                ArgumentCaptor.forClass(PasswordResetRequestDTO.class);
        verify(authService).resetPassword(request.capture());
        assertThat(request.getValue().email()).isEqualTo("Ana@Exemplo.com");
        assertThat(request.getValue().newPassword()).isEqualTo("nova-senha-456");
    }

    @Test
    void shouldIgnoreStaleBearer_whenTokenIsGarbage() throws Exception {
        // When / Then
        mockMvc.perform(
                        post(PATH)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"email": "ana@exemplo.com", "newPassword": "nova-senha-456"}
                    """))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldIgnoreUnknownFields_whenBodyCarriesRole() throws Exception {
        // When
        mockMvc.perform(
                        post(PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"email": "ana@exemplo.com", "newPassword": "nova-senha-456", "role": "ADMIN"}
                    """))
                .andExpect(status().isNoContent());

        // Then the DTO only has e-mail and new password: nothing else can reach the service
        ArgumentCaptor<PasswordResetRequestDTO> request =
                ArgumentCaptor.forClass(PasswordResetRequestDTO.class);
        verify(authService).resetPassword(request.capture());
        assertThat(PasswordResetRequestDTO.class.getRecordComponents()).hasSize(2);
    }

    @Test
    void shouldReturnValidationProblemWithoutEchoingValues_whenFieldsAreInvalid() throws Exception {
        // When
        String body =
                mockMvc.perform(
                                post(PATH)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                        {"email": "not-an-email", "newPassword": "short7!"}
                        """))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                content()
                                        .contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                        .andExpect(jsonPath("$.errors[*].field", hasItems("email", "newPassword")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(StandardCharsets.UTF_8);

        // Then
        assertThat(body).doesNotContain("short7!").doesNotContain("not-an-email");
        verifyNoInteractions(authService);
    }

    @Test
    void shouldReturnValidationProblem_whenNewPasswordExceedsBcryptBytes() throws Exception {
        // Given 60 characters (within @Size) but 120 UTF-8 bytes
        String password = "é".repeat(60);

        // When / Then
        mockMvc.perform(
                        post(PATH)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"email": "ana@exemplo.com", "newPassword": "%s"}
                    """
                                                .formatted(password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("newPassword"));

        verifyNoInteractions(authService);
    }
}
