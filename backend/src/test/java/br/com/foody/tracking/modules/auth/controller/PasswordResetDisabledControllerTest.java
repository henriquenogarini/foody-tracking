package br.com.foody.tracking.modules.auth.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.foody.tracking.config.ClockConfig;
import br.com.foody.tracking.config.JacksonConfig;
import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.config.SecurityConfig;
import br.com.foody.tracking.modules.auth.service.AuthService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = PasswordResetController.class,
    properties = "app.security.password-reset.enabled=false")
@Import({SecurityConfig.class, JwtConfig.class, ClockConfig.class, JacksonConfig.class})
@ActiveProfiles("test")
class PasswordResetDisabledControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AuthService authService;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"email\": \"ana@exemplo.com\", \"newPassword\": \"nova-senha-456\"}",
        "{\"email\": \"not-an-email\", \"newPassword\": \"1\"}",
        "{not json"
      })
  void shouldReturnNotFound_whenFeatureIsDisabled(String body) throws Exception {
    // When / Then
    mockMvc
        .perform(
            post("/api/v1/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.errors").doesNotExist());

    verifyNoInteractions(authService);
  }
}
