package br.com.foody.tracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.shared.error.ApiErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {

  private final OpenAPI openApi = new OpenApiConfig().foodyOpenApi();

  @Test
  void shouldDeclareBearerJwtScheme_whenOpenApiIsBuilt() {
    // When
    SecurityScheme scheme =
        openApi.getComponents().getSecuritySchemes().get(OpenApiConfig.BEARER_AUTH);

    // Then
    assertThat(scheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
    assertThat(scheme.getScheme()).isEqualTo("bearer");
    assertThat(scheme.getBearerFormat()).isEqualTo("JWT");
  }

  @Test
  void shouldDeclareFeatureTags_whenOpenApiIsBuilt() {
    // When / Then
    assertThat(openApi.getTags())
        .extracting(Tag::getName)
        .containsExactly("Auth", "Users", "Orders");
  }

  @Test
  void shouldDocumentEveryErrorCode_whenUsageGuideIsRendered() {
    // When
    String description = openApi.getInfo().getDescription();

    // Then
    for (ApiErrorCode code : ApiErrorCode.values()) {
      assertThat(description).contains("`" + code.name() + "` | " + code.httpStatus());
    }
  }
}
