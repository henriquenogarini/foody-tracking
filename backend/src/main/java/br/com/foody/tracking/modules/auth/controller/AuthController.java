package br.com.foody.tracking.modules.auth.controller;

import br.com.foody.tracking.config.OpenApiConfig;
import br.com.foody.tracking.modules.auth.dto.LoginRequestDTO;
import br.com.foody.tracking.modules.auth.dto.RegisterRequestDTO;
import br.com.foody.tracking.modules.auth.dto.TokenResponseDTO;
import br.com.foody.tracking.modules.auth.dto.UserResponseDTO;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.auth.service.AuthService;
import br.com.foody.tracking.shared.exception.model.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = OpenApiConfig.TAG_AUTH, description = OpenApiConfig.TAG_AUTH_DESCRIPTION)
public final class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Register a user",
      description =
          "Creates an account with role `USER` (the role cannot be chosen). The e-mail is"
              + " normalized to lowercase and must be unique; the password needs 8 to 72"
              + " characters.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "User created",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "`VALIDATION_ERROR`: invalid or missing fields, or malformed body",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description = "`USER_EMAIL_ALREADY_EXISTS`: the e-mail is already registered",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public UserResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  @Operation(
      summary = "Log in",
      description =
          "Exchanges e-mail and password for a JWT valid for 2 hours. Use the `accessToken` in"
              + " the Authorize button or as `Authorization: Bearer <token>`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Token issued",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = TokenResponseDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "`VALIDATION_ERROR`: invalid or missing fields, or malformed body",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description =
            "`AUTH_INVALID_CREDENTIALS`: unknown e-mail or wrong password (same answer for both)",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  @SecurityRequirement(name = "bearerAuth")
  @Operation(
      summary = "Get the authenticated user",
      description = "Returns the account identified by the bearer token.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The authenticated user",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserResponseDTO.class))),
    @ApiResponse(
        responseCode = "401",
        description = "`AUTH_TOKEN_INVALID`: missing, invalid or expired token",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public UserResponseDTO me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    return authService.me(AuthenticatedUser.from(jwt));
  }
}
