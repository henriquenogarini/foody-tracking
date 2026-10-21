package br.com.foody.tracking.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.auth.application.dto.UserResponse;
import br.com.foody.tracking.auth.domain.Role;
import br.com.foody.tracking.auth.domain.User;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserMapperTest {

  private static final OffsetDateTime NOW =
      OffsetDateTime.ofInstant(
          Instant.parse("2026-10-21T17:13:27Z"), ZoneId.of("America/Sao_Paulo"));

  private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

  @Test
  void shouldMapEveryField_whenConvertingUserToResponse() {
    // Given
    User user = new User("Ana Souza", "ana@exemplo.com", "hash", Role.ADMIN, NOW);

    // When
    UserResponse response = mapper.toResponse(user);

    // Then
    assertThat(response.id()).isEqualTo(user.getPublicId());
    assertThat(response.name()).isEqualTo("Ana Souza");
    assertThat(response.email()).isEqualTo("ana@exemplo.com");
    assertThat(response.role()).isEqualTo(Role.ADMIN);
    assertThat(response.createdAt()).isAtSameInstantAs(NOW);
  }

  @Test
  void shouldReturnNull_whenUserIsNull() {
    // When / Then
    assertThat(mapper.toResponse(null)).isNull();
  }
}
