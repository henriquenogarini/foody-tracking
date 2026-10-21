package br.com.foody.tracking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class OpenApiConfig {

  static final String BEARER_AUTH = "bearerAuth";

  // Rendered by Swagger UI at the top of the page: it doubles as the usage guide of the API.
  private static final String DESCRIPTION =
      """
      REST API to track Foody Delivery orders.

      ## Getting started
      1. `POST /api/v1/auth/register` with name, e-mail and password (8 to 72 characters).
      2. `POST /api/v1/auth/login` with the same e-mail and password and copy the `accessToken`.
      3. Click **Authorize**, paste the token (without the `Bearer` prefix) and confirm.
         Every other endpoint requires it; the token expires after 2 hours.

      ## Roles
      - `USER` (every registered account): sees and changes only their own orders; another \
      user's order answers 404 `ORDER_NOT_FOUND`.
      - `ADMIN`: the same order endpoints over **all** orders, plus `GET /api/v1/users`. The \
      single admin is created at startup from environment variables, never through the API.

      ## Order status
      New orders always start in `RECEBIDO`. Any transition not listed below, including to \
      the same status, answers 409 `ORDER_INVALID_TRANSITION`.

      | From | Allowed targets |
      |---|---|
      | `RECEBIDO` | `EM_PREPARO`, `CANCELADO` |
      | `EM_PREPARO` | `SAIU_PARA_ENTREGA`, `CANCELADO` |
      | `SAIU_PARA_ENTREGA` | `ENTREGUE` |
      | `ENTREGUE` | none (terminal) |
      | `CANCELADO` | none (terminal) |

      ## Errors
      Every error is an RFC 9457 `ProblemDetail` (`application/problem+json`) extended with \
      `code`, `timestamp` and, on validation errors, `errors[]` (`field`, `message`). \
      `detail` and `errors[].message` are in Portuguese.

      | Code | HTTP | When |
      |---|---|---|
      | `VALIDATION_ERROR` | 400 | Invalid fields, malformed body or unknown status |
      | `AUTH_INVALID_CREDENTIALS` | 401 | Wrong e-mail or password on login |
      | `AUTH_TOKEN_INVALID` | 401 | Missing, invalid or expired token |
      | `ACCESS_DENIED` | 403 | The role does not allow the operation |
      | `ORDER_NOT_FOUND` | 404 | The order does not exist or is not visible to the caller |
      | `USER_EMAIL_ALREADY_EXISTS` | 409 | The e-mail is already registered |
      | `ORDER_INVALID_TRANSITION` | 409 | The status change breaks the state machine |
      | `CONCURRENT_MODIFICATION` | 409 | The order was changed by another request; reload it |
      | `INTERNAL_ERROR` | 500 | Unexpected failure |

      ## Dates and money
      Dates are ISO-8601 in the application zone `America/Sao_Paulo` \
      (e.g. `2026-10-20T14:00:00-03:00`). Money values have 2 decimal places.
      """;

  @Bean
  public OpenAPI foodyOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Foody Tracking API")
                .version("v1")
                .description(DESCRIPTION)
                .contact(
                    new Contact()
                        .name("henriquenogarini")
                        .url("https://github.com/henriquenogarini/foody-tracking")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token returned by POST /api/v1/auth/login")))
        .tags(
            List.of(
                new Tag()
                    .name("Auth")
                    .description("Registration, login and the authenticated user"),
                new Tag().name("Users").description("User accounts (ADMIN only)"),
                new Tag()
                    .name("Orders")
                    .description("Delivery orders, status changes and their history")));
  }
}
