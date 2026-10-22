package br.com.foody.tracking.modules.auth.controller;

import br.com.foody.tracking.config.OpenApiConfig;
import br.com.foody.tracking.modules.auth.dto.UserResponseDTO;
import br.com.foody.tracking.modules.auth.service.UserQueryService;
import br.com.foody.tracking.shared.exception.model.ApiErrorResponse;
import br.com.foody.tracking.shared.web.PageResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = OpenApiConfig.TAG_USERS, description = OpenApiConfig.TAG_USERS_DESCRIPTION)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

  private final UserQueryService userQueryService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "List users",
      description =
          "Paged list of every account, newest first by default. Requires the `ADMIN` role.")
  // No explicit schema: @Schema(implementation = PageResponseDTO.class) would erase the generic.
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Page of users"),
    @ApiResponse(
        responseCode = "400",
        description = "`VALIDATION_ERROR`: sort property outside the allowed list",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "401",
        description = "`AUTH_TOKEN_INVALID`: missing, invalid or expired token",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "403",
        description = "`ACCESS_DENIED`: the caller is not an `ADMIN`",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public PageResponseDTO<UserResponseDTO> list(
      @ParameterObject
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return userQueryService.list(pageable);
  }
}
