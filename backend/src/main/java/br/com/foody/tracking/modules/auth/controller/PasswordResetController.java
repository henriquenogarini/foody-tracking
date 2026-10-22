package br.com.foody.tracking.modules.auth.controller;

import br.com.foody.tracking.config.OpenApiConfig;
import br.com.foody.tracking.modules.auth.dto.PasswordResetRequestDTO;
import br.com.foody.tracking.modules.auth.service.AuthService;
import br.com.foody.tracking.shared.exception.model.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// Own controller so the flag removes the route: when disabled, 404 comes before any body parsing.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = OpenApiConfig.TAG_AUTH, description = OpenApiConfig.TAG_AUTH_DESCRIPTION)
@ConditionalOnProperty(
    prefix = "app.security.password-reset",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public final class PasswordResetController {

  private final AuthService authService;

  @PostMapping("/password-reset")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Reset a password (demo)",
      description =
          "Demo feature without e-mail confirmation: sets a new password for the account with the"
              + " given e-mail. Always answers 204, whether or not the e-mail exists; the admin"
              + " account is never changed. Tokens already issued stay valid until they expire."
              + " Can be disabled with `APP_SECURITY_PASSWORD_RESET_ENABLED=false`.")
  @ApiResponses({
    @ApiResponse(responseCode = "204", description = "Request accepted (no body)"),
    @ApiResponse(
        responseCode = "400",
        description = "`VALIDATION_ERROR`: invalid or missing fields, or malformed body",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public void resetPassword(@Valid @RequestBody PasswordResetRequestDTO request) {
    authService.resetPassword(request);
  }
}
