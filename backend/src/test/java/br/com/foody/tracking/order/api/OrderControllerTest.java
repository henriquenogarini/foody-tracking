package br.com.foody.tracking.order.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.foody.tracking.auth.application.AuthenticatedUser;
import br.com.foody.tracking.auth.domain.Role;
import br.com.foody.tracking.config.ClockConfig;
import br.com.foody.tracking.config.JacksonConfig;
import br.com.foody.tracking.config.JwtConfig;
import br.com.foody.tracking.config.SecurityConfig;
import br.com.foody.tracking.order.application.OrderNotFoundException;
import br.com.foody.tracking.order.application.OrderQueryService;
import br.com.foody.tracking.order.application.OrderService;
import br.com.foody.tracking.order.application.dto.AddressResponse;
import br.com.foody.tracking.order.application.dto.CustomerResponse;
import br.com.foody.tracking.order.application.dto.OrderItemResponse;
import br.com.foody.tracking.order.application.dto.OrderResponse;
import br.com.foody.tracking.order.application.dto.OrderSummaryResponse;
import br.com.foody.tracking.order.application.dto.StatusHistoryResponse;
import br.com.foody.tracking.order.application.dto.UpdateStatusRequest;
import br.com.foody.tracking.order.domain.InvalidStatusTransitionException;
import br.com.foody.tracking.order.domain.OrderStatus;
import br.com.foody.tracking.shared.web.InvalidSortException;
import br.com.foody.tracking.shared.web.PageResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, JwtConfig.class, ClockConfig.class, JacksonConfig.class})
@ActiveProfiles("test")
class OrderControllerTest {

  private static final UUID USER_ID = UUID.fromString("3f6c1a2e-8d4b-4c7a-9e1f-2b5d7a9c0e11");
  private static final UUID ADMIN_ID = UUID.fromString("0b6f2c1d-4e3a-4f5b-8c7d-9e0a1b2c3d4e");
  private static final UUID ORDER_ID = UUID.fromString("7b1e4c2a-5d3f-4a8b-9c6d-1e2f3a4b5c6d");
  // Stored in UTC; the JSON boundary must re-zone it to America/Sao_Paulo.
  private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-21T17:13:27Z");

  private static final String VALID_ORDER =
      """
      {
        "customer": {"name": "José da Silva", "phone": "+5511987521144", "email": "jose@exemplo.com"},
        "items": [{"description": "Pizza calabresa G", "quantity": 1, "unitPrice": 59.90}],
        "deliveryAddress": {
          "street": "Rua José Neves", "number": "590", "complement": "sala 03",
          "neighborhood": "Vila São Paulo", "city": "São Paulo", "state": "SP",
          "postalCode": "04650-140", "lat": -23.6578, "lng": -46.6743
        },
        "notes": "sem cebola"
      }
      """;

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OrderService orderService;
  @MockitoBean private OrderQueryService orderQueryService;

  @Test
  void shouldReturnCreatedOrderWithLocation_whenRequestIsValid() throws Exception {
    // Given
    when(orderService.create(any(), any())).thenReturn(orderResponse(OrderStatus.RECEBIDO));

    // When
    mockMvc
        .perform(
            post("/api/v1/orders")
                .queryParam("x", "1")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_ORDER))
        .andExpect(status().isCreated())
        // The query string of the POST must never leak into the Location of the new order.
        .andExpect(header().string("Location", "http://localhost/api/v1/orders/" + ORDER_ID))
        .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
        .andExpect(jsonPath("$.code").value(1042))
        .andExpect(jsonPath("$.status").value("RECEBIDO"))
        .andExpect(jsonPath("$.total").value(59.90))
        .andExpect(jsonPath("$.items[0].subtotal").value(59.90))
        .andExpect(jsonPath("$.customer.name").value("José da Silva"))
        .andExpect(jsonPath("$.deliveryAddress.city").value("São Paulo"))
        .andExpect(jsonPath("$.createdAt").value("2026-10-21T14:13:27-03:00"))
        .andExpect(jsonPath("$.updatedAt").value("2026-10-21T14:13:27-03:00"));

    // Then
    ArgumentCaptor<AuthenticatedUser> actor = ArgumentCaptor.forClass(AuthenticatedUser.class);
    verify(orderService).create(any(), actor.capture());
    assertThat(actor.getValue()).isEqualTo(new AuthenticatedUser(USER_ID, Role.USER));
  }

  @Test
  void shouldReturnFieldErrors_whenCreateRequestIsInvalid() throws Exception {
    // Given: zero price, blank customer name, bad UF and lat without lng
    String invalid =
        """
        {
          "customer": {"name": " "},
          "items": [{"description": "Pizza", "quantity": 1, "unitPrice": 0.00}],
          "deliveryAddress": {
            "street": "Rua José Neves", "number": "590", "neighborhood": "Vila São Paulo",
            "city": "São Paulo", "state": "sp", "postalCode": "04650-140", "lat": -23.6578
          }
        }
        """;

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalid))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.errors[*].field")
                .value(
                    hasItems(
                        "items[0].unitPrice",
                        "customer.name",
                        "deliveryAddress.state",
                        "deliveryAddress.lng")));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnUnitPriceError_whenUnitPriceExceedsMaximum() throws Exception {
    // Given
    String tooExpensive = VALID_ORDER.replace("\"unitPrice\": 59.90", "\"unitPrice\": 100000.00");

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(tooExpensive))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[*].field").value(hasItems("items[0].unitPrice")));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldAcceptOrder_whenUnitPriceIsAtMaximum() throws Exception {
    // Given
    when(orderService.create(any(), any())).thenReturn(orderResponse(OrderStatus.RECEBIDO));
    String atMaximum = VALID_ORDER.replace("\"unitPrice\": 59.90", "\"unitPrice\": 99999.99");

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(atMaximum))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldAcceptOrder_whenOptionalPhoneIsEmpty() throws Exception {
    // Given
    when(orderService.create(any(), any())).thenReturn(orderResponse(OrderStatus.RECEBIDO));
    String emptyPhone = VALID_ORDER.replace("\"+5511987521144\"", "\"\"");

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyPhone))
        .andExpect(status().isCreated());
  }

  @Test
  void shouldReturnSingleErrorPerField_whenStateAndPostalCodeAreBlank() throws Exception {
    // Given
    String blank =
        VALID_ORDER
            .replace("\"state\": \"SP\"", "\"state\": \" \"")
            .replace("\"04650-140\"", "\"\"");

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(blank))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(
            jsonPath("$.errors[?(@.field == 'deliveryAddress.state')].message")
                .value(contains("deve ser a sigla da UF com 2 letras maiúsculas")))
        .andExpect(
            jsonPath("$.errors[?(@.field == 'deliveryAddress.postalCode')].message")
                .value(contains("deve estar no formato 00000-000 ou 00000000")));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnItemsError_whenItemsListIsEmpty() throws Exception {
    // Given
    String noItems = VALID_ORDER.replaceFirst("\"items\": \\[.*\\],", "\"items\": [],");

    // When / Then
    mockMvc
        .perform(
            post("/api/v1/orders")
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(noItems))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("items"))
        .andExpect(jsonPath("$.errors[0].message").value("deve conter ao menos 1 item"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenCreatingWithoutToken() throws Exception {
    // When / Then
    mockMvc
        .perform(
            post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldListWithDefaultPagingAndUserScope_whenCallerIsUser() throws Exception {
    // Given
    when(orderQueryService.list(any(), any(), any()))
        .thenReturn(new PageResponse<>(List.of(summary()), 0, 20, 1, 1));

    // When
    mockMvc
        .perform(get("/api/v1/orders").with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(ORDER_ID.toString()))
        .andExpect(jsonPath("$.content[0].customerName").value("José da Silva"))
        .andExpect(jsonPath("$.content[0].itemsCount").value(1))
        .andExpect(jsonPath("$.content[0].city").value("São Paulo"))
        .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-21T14:13:27-03:00"))
        .andExpect(jsonPath("$.totalElements").value(1));

    // Then
    ArgumentCaptor<AuthenticatedUser> actor = ArgumentCaptor.forClass(AuthenticatedUser.class);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(orderQueryService).list(actor.capture(), isNull(), pageable.capture());
    assertThat(actor.getValue()).isEqualTo(new AuthenticatedUser(USER_ID, Role.USER));
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt"))
        .isNotNull()
        .extracting(Sort.Order::getDirection)
        .isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void shouldPassStatusFilter_whenStatusParamIsGiven() throws Exception {
    // Given
    when(orderQueryService.list(any(), any(), any()))
        .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

    // When
    mockMvc
        .perform(get("/api/v1/orders").param("status", "EM_PREPARO").with(userJwt()))
        .andExpect(status().isOk());

    // Then
    verify(orderQueryService).list(any(), eq(OrderStatus.EM_PREPARO), any());
  }

  @Test
  void shouldReturnValidationProblem_whenStatusParamIsUnknown() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/api/v1/orders").param("status", "FOO").with(userJwt()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("status"));

    verifyNoInteractions(orderQueryService);
  }

  @Test
  void shouldReturnValidationProblemOnSort_whenSortPropertyIsNotAllowed() throws Exception {
    // Given
    when(orderQueryService.list(any(), any(), any()))
        .thenThrow(new InvalidSortException("owner.email"));

    // When / Then
    mockMvc
        .perform(get("/api/v1/orders").param("sort", "owner.email").with(userJwt()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("sort"));
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenListingWithoutToken() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/api/v1/orders"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

    verifyNoInteractions(orderQueryService);
  }

  @Test
  void shouldPassAdminRole_whenCallerIsAdmin() throws Exception {
    // Given
    when(orderQueryService.list(any(), any(), any()))
        .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

    // When
    mockMvc.perform(get("/api/v1/orders").with(adminJwt())).andExpect(status().isOk());

    // Then
    ArgumentCaptor<AuthenticatedUser> actor = ArgumentCaptor.forClass(AuthenticatedUser.class);
    verify(orderQueryService).list(actor.capture(), isNull(), any());
    assertThat(actor.getValue()).isEqualTo(new AuthenticatedUser(ADMIN_ID, Role.ADMIN));
  }

  @Test
  void shouldReturnOrder_whenFetchingById() throws Exception {
    // Given
    when(orderQueryService.get(ORDER_ID, new AuthenticatedUser(USER_ID, Role.USER)))
        .thenReturn(orderResponse(OrderStatus.RECEBIDO));

    // When / Then
    mockMvc
        .perform(get("/api/v1/orders/{id}", ORDER_ID).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
        .andExpect(jsonPath("$.notes").value("sem cebola"));
  }

  @Test
  void shouldReturnNotFoundProblem_whenOrderBelongsToAnotherUser() throws Exception {
    // Given
    when(orderQueryService.get(any(), any())).thenThrow(new OrderNotFoundException());

    // When / Then
    mockMvc
        .perform(get("/api/v1/orders/{id}", ORDER_ID).with(userJwt()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenFetchingByIdWithoutToken() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/api/v1/orders/{id}", ORDER_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

    verifyNoInteractions(orderQueryService);
  }

  @Test
  void shouldReturnValidationProblem_whenPathIdIsNotUuid() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/api/v1/orders/{id}", "not-a-uuid").with(userJwt()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("publicId"));

    verifyNoInteractions(orderQueryService);
  }

  @Test
  void shouldReturnUpdatedOrder_whenStatusChangeIsAllowed() throws Exception {
    // Given
    when(orderService.changeStatus(any(), any(), any()))
        .thenReturn(orderResponse(OrderStatus.EM_PREPARO));

    // When
    mockMvc
        .perform(
            patch("/api/v1/orders/{id}/status", ORDER_ID)
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"EM_PREPARO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EM_PREPARO"));

    // Then
    verify(orderService)
        .changeStatus(
            ORDER_ID,
            new UpdateStatusRequest(OrderStatus.EM_PREPARO),
            new AuthenticatedUser(USER_ID, Role.USER));
  }

  @Test
  void shouldReturnConflictProblem_whenTransitionIsNotAllowed() throws Exception {
    // Given
    when(orderService.changeStatus(any(), any(), any()))
        .thenThrow(
            new InvalidStatusTransitionException(
                OrderStatus.SAIU_PARA_ENTREGA, OrderStatus.CANCELADO));

    // When / Then
    mockMvc
        .perform(
            patch("/api/v1/orders/{id}/status", ORDER_ID)
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"CANCELADO\"}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"))
        .andExpect(
            jsonPath("$.detail")
                .value("Transição de SAIU_PARA_ENTREGA para CANCELADO não é permitida"));
  }

  @Test
  void shouldReturnConflictProblem_whenTargetIsTheCurrentStatus() throws Exception {
    // Given
    when(orderService.changeStatus(any(), any(), any()))
        .thenThrow(
            new InvalidStatusTransitionException(OrderStatus.EM_PREPARO, OrderStatus.EM_PREPARO));

    // When / Then
    mockMvc
        .perform(
            patch("/api/v1/orders/{id}/status", ORDER_ID)
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"EM_PREPARO\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"))
        .andExpect(
            jsonPath("$.detail").value("Transição de EM_PREPARO para EM_PREPARO não é permitida"));
  }

  @Test
  void shouldReturnNotFoundProblem_whenChangingStatusOfOrderOutsideScope() throws Exception {
    // Given
    when(orderService.changeStatus(any(), any(), any())).thenThrow(new OrderNotFoundException());

    // When / Then
    mockMvc
        .perform(
            patch("/api/v1/orders/{id}/status", ORDER_ID)
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"EM_PREPARO\"}"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenChangingStatusWithoutToken() throws Exception {
    // When / Then
    mockMvc
        .perform(
            patch("/api/v1/orders/{id}/status", ORDER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"EM_PREPARO\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnValidationProblemWithAcceptedValues_whenBodyStatusIsUnknown() throws Exception {
    // When / Then
    mockMvc
        .perform(
            patch("/api/v1/orders/{id}/status", ORDER_ID)
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"VOANDO\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("status"))
        .andExpect(
            jsonPath("$.errors[0].message")
                .value(
                    "valor inválido; valores aceitos: RECEBIDO, EM_PREPARO, SAIU_PARA_ENTREGA,"
                        + " ENTREGUE, CANCELADO"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnValidationProblem_whenBodyStatusIsMissing() throws Exception {
    // When / Then
    mockMvc
        .perform(
            patch("/api/v1/orders/{id}/status", ORDER_ID)
                .with(userJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("status"));

    verifyNoInteractions(orderService);
  }

  @Test
  void shouldReturnHistory_whenOrderIsInScope() throws Exception {
    // Given
    when(orderQueryService.history(ORDER_ID, new AuthenticatedUser(USER_ID, Role.USER)))
        .thenReturn(
            List.of(
                new StatusHistoryResponse(null, OrderStatus.RECEBIDO, CREATED_AT, USER_ID),
                new StatusHistoryResponse(
                    OrderStatus.RECEBIDO,
                    OrderStatus.EM_PREPARO,
                    CREATED_AT.plusMinutes(5),
                    USER_ID)));

    // When / Then
    mockMvc
        .perform(get("/api/v1/orders/{id}/history", ORDER_ID).with(userJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].fromStatus").doesNotExist())
        .andExpect(jsonPath("$[0].toStatus").value("RECEBIDO"))
        .andExpect(jsonPath("$[1].fromStatus").value("RECEBIDO"))
        .andExpect(jsonPath("$[1].toStatus").value("EM_PREPARO"))
        .andExpect(jsonPath("$[1].changedAt").value("2026-10-21T14:18:27-03:00"))
        .andExpect(jsonPath("$[1].changedBy").value(USER_ID.toString()));
  }

  @Test
  void shouldReturnNotFoundProblem_whenReadingHistoryOfOrderOutsideScope() throws Exception {
    // Given
    when(orderQueryService.history(any(), any())).thenThrow(new OrderNotFoundException());

    // When / Then
    mockMvc
        .perform(get("/api/v1/orders/{id}/history", ORDER_ID).with(userJwt()))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
  }

  @Test
  void shouldReturnUnauthorizedProblem_whenReadingHistoryWithoutToken() throws Exception {
    // When / Then
    mockMvc
        .perform(get("/api/v1/orders/{id}/history", ORDER_ID))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));

    verifyNoInteractions(orderQueryService);
  }

  private static RequestPostProcessor userJwt() {
    return jwt()
        .jwt(token -> token.subject(USER_ID.toString()).claim("role", "USER"))
        .authorities(new SimpleGrantedAuthority("ROLE_USER"));
  }

  private static RequestPostProcessor adminJwt() {
    return jwt()
        .jwt(token -> token.subject(ADMIN_ID.toString()).claim("role", "ADMIN"))
        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
  }

  private static OrderResponse orderResponse(OrderStatus status) {
    BigDecimal price = new BigDecimal("59.90");
    return new OrderResponse(
        ORDER_ID,
        1042L,
        status,
        new CustomerResponse("José da Silva", "+5511987521144", "jose@exemplo.com"),
        List.of(new OrderItemResponse("Pizza calabresa G", 1, price, price)),
        new AddressResponse(
            "Rua José Neves",
            "590",
            "sala 03",
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650-140",
            -23.6578,
            -46.6743),
        "sem cebola",
        price,
        CREATED_AT,
        CREATED_AT);
  }

  private static OrderSummaryResponse summary() {
    return new OrderSummaryResponse(
        ORDER_ID,
        1042L,
        OrderStatus.RECEBIDO,
        "José da Silva",
        new BigDecimal("59.90"),
        1,
        "São Paulo",
        CREATED_AT,
        CREATED_AT);
  }
}
