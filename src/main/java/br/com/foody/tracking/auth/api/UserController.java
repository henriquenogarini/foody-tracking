package br.com.foody.tracking.auth.api;

import br.com.foody.tracking.auth.application.UserQueryService;
import br.com.foody.tracking.auth.application.dto.UserResponse;
import br.com.foody.tracking.shared.error.ApiErrorResponse;
import br.com.foody.tracking.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Not final: @PreAuthorize needs a CGLIB subclass proxy.
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User accounts (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private final UserQueryService userQueryService;

  public UserController(UserQueryService userQueryService) {
    this.userQueryService = userQueryService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(
      summary = "List users",
      description =
          "Paged list of every account, newest first by default. Requires the `ADMIN` role.")
  // The 200 has no explicit schema: springdoc infers the generic PageResponse<UserResponse> from
  // the return type, which @Schema(implementation = PageResponse.class) would erase.
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Page of users"),
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
  public PageResponse<UserResponse> list(
      @ParameterObject
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return userQueryService.list(pageable);
  }
}
