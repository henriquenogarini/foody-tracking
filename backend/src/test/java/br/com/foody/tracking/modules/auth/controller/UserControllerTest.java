package br.com.foody.tracking.modules.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.foody.tracking.config.ClockConfig;
import br.com.foody.tracking.config.JacksonConfig;
import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.config.SecurityConfig;
import br.com.foody.tracking.modules.auth.dto.UserResponse;
import br.com.foody.tracking.modules.auth.enums.Role;
import br.com.foody.tracking.modules.auth.service.UserQueryService;
import br.com.foody.tracking.shared.web.PageResponse;
import br.com.foody.tracking.shared.web.SortWhitelist;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JwtConfig.class, ClockConfig.class, JacksonConfig.class})
@ActiveProfiles("test")
class UserControllerTest {

  private static final UUID ADMIN_ID = UUID.fromString("0b6f2c1d-4e3a-4f5b-8c7d-9e0a1b2c3d4e");
  private static final UUID USER_ID = UUID.fromString("3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserQueryService userQueryService;

  @Test
  void shouldReturnPageOfUsersWithDefaultPaging_whenCallerIsAdmin() throws Exception {
    // Given
    UserResponse user =
        new UserResponse(
            USER_ID,
            "Ana Souza",
            "ana@exemplo.com",
            Role.USER,
            OffsetDateTime.parse("2026-10-21T17:13:27Z"));
    when(userQueryService.list(any())).thenReturn(new PageResponse<>(List.of(user), 0, 20, 1, 1));

    // When
    mockMvc
        .perform(
            get("/api/v1/users")
                .with(
                    jwt()
                        .jwt(token -> token.subject(ADMIN_ID.toString()).claim("role", "ADMIN"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(USER_ID.toString()))
        .andExpect(jsonPath("$.content[0].role").value("USER"))
        .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-21T14:13:27-03:00"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.totalPages").value(1));

    // Then
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(userQueryService).list(pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void shouldReturnSortValidationProblem_whenSortPropertyIsNotWhitelisted() throws Exception {
    // Given: the mock applies the real whitelist to the Pageable the controller resolved
    when(userQueryService.list(any()))
        .thenAnswer(
            invocation -> {
              Pageable pageable = invocation.getArgument(0);
              SortWhitelist.requireAllowed(
                  pageable.getSort(), Set.of("createdAt", "name", "email"));
              return new PageResponse<UserResponse>(List.of(), 0, 20, 0, 0);
            });

    // When / Then
    mockMvc
        .perform(
            get("/api/v1/users")
                .param("sort", "passwordHash,asc")
                .with(
                    jwt()
                        .jwt(token -> token.subject(ADMIN_ID.toString()).claim("role", "ADMIN"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value("sort"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value("propriedade de ordenação inválida: passwordHash"));
  }

  @Test
  void shouldReturnForbiddenProblem_whenCallerIsUser() throws Exception {
    // When / Then
    mockMvc
        .perform(
            get("/api/v1/users")
                .with(
                    jwt()
                        .jwt(token -> token.subject(USER_ID.toString()).claim("role", "USER"))
                        .authorities(new SimpleGrantedAuthority("ROLE_USER"))))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    verifyNoInteractions(userQueryService);
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenTokenIsMissing() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/api/v1/users"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

    verifyNoInteractions(userQueryService);
  }
}
