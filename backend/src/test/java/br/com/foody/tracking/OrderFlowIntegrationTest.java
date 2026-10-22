package br.com.foody.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
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

// `flowdb` (shared with PasswordResetIntegrationTest) keeps committed rows out of `memdb`.
// E-mails are unique per run and assertions relative, so rows from other classes do not matter.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties =
        "spring.datasource.url=jdbc:sqlite:file:flowdb?mode=memory&cache=shared&foreign_keys=true")
class OrderFlowIntegrationTest {

  private static final String PASSWORD = "senha-forte-123";

  private static final String ORDER_PAYLOAD =
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

  @Value("${app.security.admin.email}")
  private String adminEmail;

  @Value("${app.security.admin.password}")
  private String adminPassword;

  @Test
  void shouldRunTheWholeOrderLifecycle_whenUsersAndAdminUseTheApi() throws Exception {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String anaEmail = "ana-" + suffix + "@exemplo.com";
    String brunoEmail = "bruno-" + suffix + "@exemplo.com";

    // Given Ana registers and logs in
    String anaId =
        read(
            register("Ana Souza", anaEmail)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.email").value(anaEmail)),
            "$.id");
    String anaToken = login(anaEmail, PASSWORD);
    mockMvc
        .perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(anaToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(anaId));

    // When Ana creates an order
    ResultActions created =
        mockMvc
            .perform(
                post("/api/v1/orders")
                    .header(HttpHeaders.AUTHORIZATION, bearer(anaToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(ORDER_PAYLOAD))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RECEBIDO"))
            .andExpect(jsonPath("$.total").value(59.90))
            .andExpect(jsonPath("$.createdAt").value(endsWith("-03:00")));
    String orderId = read(created, "$.id");
    String orderPath = "/api/v1/orders/" + orderId;
    created.andExpect(header().string(HttpHeaders.LOCATION, endsWith(orderPath)));

    // And a later GET returns the same timestamps (the Clock is truncated to the stored millis)
    mockMvc
        .perform(get(orderPath).header(HttpHeaders.AUTHORIZATION, bearer(anaToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.createdAt").value(read(created, "$.createdAt")))
        .andExpect(jsonPath("$.updatedAt").value(read(created, "$.updatedAt")));

    // Then she advances it along the state machine
    changeStatus(anaToken, orderPath, "EM_PREPARO")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("EM_PREPARO"));
    changeStatus(anaToken, orderPath, "SAIU_PARA_ENTREGA").andExpect(status().isOk());

    // And forbidden or same-status transitions answer 409
    changeStatus(anaToken, orderPath, "CANCELADO")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"));
    changeStatus(anaToken, orderPath, "SAIU_PARA_ENTREGA")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ORDER_INVALID_TRANSITION"));

    // And the history holds one row per accepted transition, in order, by Ana
    mockMvc
        .perform(get(orderPath + "/history").header(HttpHeaders.AUTHORIZATION, bearer(anaToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].toStatus").value("RECEBIDO"))
        .andExpect(jsonPath("$[1].toStatus").value("EM_PREPARO"))
        .andExpect(jsonPath("$[2].fromStatus").value("EM_PREPARO"))
        .andExpect(jsonPath("$[2].toStatus").value("SAIU_PARA_ENTREGA"))
        .andExpect(jsonPath("$[*].changedBy").value(everyItem(equalTo(anaId))))
        .andExpect(jsonPath("$[*].changedByName").value(everyItem(equalTo("Ana Souza"))));

    // Given Bruno, another USER
    register("Bruno Lima", brunoEmail).andExpect(status().isCreated());
    String brunoToken = login(brunoEmail, PASSWORD);

    // Then Ana's order does not exist for him (404, never 403) and is not in his list
    mockMvc
        .perform(get(orderPath).header(HttpHeaders.AUTHORIZATION, bearer(brunoToken)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    mockMvc
        .perform(
            get("/api/v1/orders")
                .param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(brunoToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].id").value(not(hasItem(orderId))));
    // And he can neither change its status nor read its history
    changeStatus(brunoToken, orderPath, "ENTREGUE")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    mockMvc
        .perform(get(orderPath + "/history").header(HttpHeaders.AUTHORIZATION, bearer(brunoToken)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

    // And the user listing is admin-only
    mockMvc
        .perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(brunoToken)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

    // Given the bootstrapped admin
    String adminToken = login(adminEmail, adminPassword);
    String adminId =
        read(
            mockMvc
                .perform(
                    get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN")),
            "$.id");

    // Then the admin sees Ana's order in the unscoped list
    mockMvc
        .perform(
            get("/api/v1/orders")
                .param("status", "SAIU_PARA_ENTREGA")
                .param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].id").value(hasItem(orderId)));

    // And can deliver it, recorded under the admin's id
    changeStatus(adminToken, orderPath, "ENTREGUE")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ENTREGUE"));
    mockMvc
        .perform(get(orderPath + "/history").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$[3].toStatus").value("ENTREGUE"))
        .andExpect(jsonPath("$[3].changedBy").value(adminId));

    // And lists the users, Ana and Bruno included
    mockMvc
        .perform(
            get("/api/v1/users")
                .param("size", "100")
                .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[*].email").value(hasItems(anaEmail, brunoEmail)));

    mockMvc
        .perform(get("/api/v1/orders"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
  }

  @Test
  void shouldServePublicOpenApiDocs_whenFullContextLoads() throws Exception {
    // Given the full context with springdoc
    // When the docs are fetched without a token
    String body =
        mockMvc
            .perform(get("/v3/api-docs"))
            // Then they are public and describe the bearer scheme and the order routes
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.components.securitySchemes", hasKey("bearerAuth")))
            .andExpect(jsonPath("$.paths", hasKey("/api/v1/orders")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).contains("/api/v1/orders/{publicId}/status");
    // And each tag is declared exactly once
    List<String> tagNames = JsonPath.read(body, "$.tags[*].name");
    assertThat(tagNames).containsExactlyInAnyOrder("Auth", "Users", "Orders");
  }

  private ResultActions register(String name, String email) throws Exception {
    return mockMvc.perform(
        post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"name": "%s", "email": "%s", "password": "%s"}
                """
                    .formatted(name, email, PASSWORD)));
  }

  private String login(String email, String password) throws Exception {
    return read(
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email": "%s", "password": "%s"}
                        """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer")),
        "$.accessToken");
  }

  private ResultActions changeStatus(String token, String orderPath, String target)
      throws Exception {
    return mockMvc.perform(
        patch(orderPath + "/status")
            .header(HttpHeaders.AUTHORIZATION, bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\": \"%s\"}".formatted(target)));
  }

  private static String read(ResultActions result, String path) throws Exception {
    return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
  }

  private static String bearer(String token) {
    return "Bearer " + token;
  }
}
