package br.com.foody.tracking.modules.order.controller;

import br.com.foody.tracking.config.OpenApiConfig;
import br.com.foody.tracking.modules.auth.security.AuthenticatedUser;
import br.com.foody.tracking.modules.order.dto.CreateOrderRequestDTO;
import br.com.foody.tracking.modules.order.dto.OrderResponseDTO;
import br.com.foody.tracking.modules.order.dto.OrderSummaryResponseDTO;
import br.com.foody.tracking.modules.order.dto.StatusHistoryResponseDTO;
import br.com.foody.tracking.modules.order.dto.UpdateStatusRequestDTO;
import br.com.foody.tracking.modules.order.enums.OrderStatus;
import br.com.foody.tracking.modules.order.service.OrderQueryService;
import br.com.foody.tracking.modules.order.service.OrderService;
import br.com.foody.tracking.shared.exception.model.ApiErrorResponse;
import br.com.foody.tracking.shared.web.PageResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = OpenApiConfig.TAG_ORDERS, description = OpenApiConfig.TAG_ORDERS_DESCRIPTION)
@SecurityRequirement(name = "bearerAuth")
public final class OrderController {

  private static final String PUBLIC_ID_DESCRIPTION = "Public identifier of the order";
  private static final String PUBLIC_ID_EXAMPLE = "7b1e4c2a-5d3f-4a8b-9c6d-1e2f3a4b5c6d";

  private final OrderService orderService;
  private final OrderQueryService orderQueryService;

  public OrderController(OrderService orderService, OrderQueryService orderQueryService) {
    this.orderService = orderService;
    this.orderQueryService = orderQueryService;
  }

  @PostMapping
  @Operation(
      summary = "Create an order",
      description =
          "Creates an order owned by the caller. It is always born in `RECEBIDO`; `total` and"
              + " each item `subtotal` are computed by the server.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Order created; `Location` points to the new order",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = OrderResponseDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "`VALIDATION_ERROR`: invalid or missing fields, or malformed body",
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
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public ResponseEntity<OrderResponseDTO> create(
      @Valid @RequestBody CreateOrderRequestDTO request,
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    OrderResponseDTO response = orderService.create(request, AuthenticatedUser.from(jwt));
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  @Operation(
      summary = "List orders",
      description =
          "Paged order summaries, newest first by default, optionally filtered by status and by"
              + " a search term `q`: a number (optionally `#`-prefixed) matches the order code"
              + " exactly; any other text matches customer names containing it, ignoring case"
              + " (ASCII letters only). A `USER` sees only their own orders; an `ADMIN` sees every"
              + " order. Sortable by `createdAt`, `updatedAt`, `code`, `status` and `total`.")
  // The 200 has no explicit schema: springdoc infers the generic PageResponseDTO<...> from the
  // return type, which @Schema(implementation = PageResponseDTO.class) would erase.
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Page of order summaries"),
    @ApiResponse(
        responseCode = "400",
        description =
            "`VALIDATION_ERROR`: unknown status or sort property, or `q` longer than 100"
                + " characters",
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
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public PageResponseDTO<OrderSummaryResponseDTO> list(
      @Parameter(description = "Only orders in this status", example = "EM_PREPARO")
          @RequestParam(required = false)
          OrderStatus status,
      @Parameter(
              description =
                  "Search term: order code (`1042` or `#1042`) or part of the customer name;"
                      + " blank means no filter",
              example = "#1042")
          @RequestParam(required = false)
          @Size(max = 100, message = "deve ter no máximo 100 caracteres")
          String q,
      @ParameterObject
          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable,
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    return orderQueryService.list(AuthenticatedUser.from(jwt), status, q, pageable);
  }

  @GetMapping("/{publicId}")
  @Operation(
      summary = "Get an order",
      description =
          "Full view of one order. A `USER` reaches only their own orders (another user's order"
              + " answers 404, not 403, to avoid revealing it); an `ADMIN` reaches any order.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "The order",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = OrderResponseDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "`VALIDATION_ERROR`: `publicId` is not a valid UUID",
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
        responseCode = "404",
        description = "`ORDER_NOT_FOUND`: no such order within the caller's scope",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public OrderResponseDTO get(
      @Parameter(description = PUBLIC_ID_DESCRIPTION, example = PUBLIC_ID_EXAMPLE) @PathVariable
          UUID publicId,
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    return orderQueryService.get(publicId, AuthenticatedUser.from(jwt));
  }

  @PatchMapping("/{publicId}/status")
  @Operation(
      summary = "Change the order status",
      description =
          "Moves the order along the state machine: `RECEBIDO -> EM_PREPARO | CANCELADO`,"
              + " `EM_PREPARO -> SAIU_PARA_ENTREGA | CANCELADO`, `SAIU_PARA_ENTREGA -> ENTREGUE`;"
              + " `ENTREGUE` and `CANCELADO` are terminal and the same status is rejected. A"
              + " `USER` changes only their own orders (another user's order answers 404); an"
              + " `ADMIN` changes any order. The history records the caller as `changedBy`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Status changed",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = OrderResponseDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description =
            "`VALIDATION_ERROR`: `publicId` is not a valid UUID, missing or unknown status, or"
                + " malformed body",
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
        responseCode = "404",
        description = "`ORDER_NOT_FOUND`: no such order within the caller's scope",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class))),
    @ApiResponse(
        responseCode = "409",
        description =
            "`ORDER_INVALID_TRANSITION`: the state machine forbids the move;"
                + " `CONCURRENT_MODIFICATION`: the order changed concurrently",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public OrderResponseDTO changeStatus(
      @Parameter(description = PUBLIC_ID_DESCRIPTION, example = PUBLIC_ID_EXAMPLE) @PathVariable
          UUID publicId,
      @Valid @RequestBody UpdateStatusRequestDTO request,
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    return orderService.changeStatus(publicId, request, AuthenticatedUser.from(jwt));
  }

  @GetMapping("/{publicId}/history")
  @Operation(
      summary = "Get the status history",
      description =
          "Every status change of the order, oldest first, starting with its creation in"
              + " `RECEBIDO`. Same scope as fetching the order: another user's order answers"
              + " 404 for a `USER`.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Status history, oldest first",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array =
                    @ArraySchema(
                        schema = @Schema(implementation = StatusHistoryResponseDTO.class)))),
    @ApiResponse(
        responseCode = "400",
        description = "`VALIDATION_ERROR`: `publicId` is not a valid UUID",
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
        responseCode = "404",
        description = "`ORDER_NOT_FOUND`: no such order within the caller's scope",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ApiErrorResponse.class)))
  })
  public List<StatusHistoryResponseDTO> history(
      @Parameter(description = PUBLIC_ID_DESCRIPTION, example = PUBLIC_ID_EXAMPLE) @PathVariable
          UUID publicId,
      @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
    return orderQueryService.history(publicId, AuthenticatedUser.from(jwt));
  }
}
