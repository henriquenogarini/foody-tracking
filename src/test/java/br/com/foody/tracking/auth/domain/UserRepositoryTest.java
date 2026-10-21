package br.com.foody.tracking.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTest {

  private static final ZoneId APP_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final OffsetDateTime NOW =
      OffsetDateTime.ofInstant(Instant.parse("2026-10-21T17:13:27Z"), APP_ZONE);

  @Autowired private UserRepository userRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManager entityManager;

  @Test
  void shouldRoundTripPublicIdRoleAndCreatedAt_whenUserIsSavedAndReloaded() {
    // Given
    User saved =
        userRepository.saveAndFlush(
            new User("Ana Souza", "ana@exemplo.com", "hash", Role.ADMIN, NOW));
    entityManager.clear();

    // When
    User reloaded = userRepository.findByPublicId(saved.getPublicId()).orElseThrow();

    // Then
    assertThat(reloaded.getId()).isEqualTo(saved.getId());
    assertThat(reloaded.getPublicId()).isEqualTo(saved.getPublicId());
    assertThat(reloaded.getName()).isEqualTo("Ana Souza");
    assertThat(reloaded.getEmail()).isEqualTo("ana@exemplo.com");
    assertThat(reloaded.getPasswordHash()).isEqualTo("hash");
    assertThat(reloaded.getRole()).isEqualTo(Role.ADMIN);
    assertThat(reloaded.getCreatedAt()).isAtSameInstantAs(NOW);
  }

  @Test
  void shouldReturnEmpty_whenPublicIdIsUnknown() {
    // When / Then
    assertThat(userRepository.findByPublicId(UUID.randomUUID())).isEmpty();
  }

  @Test
  void shouldFindAndReportExistence_whenSearchingByEmail() {
    // Given
    userRepository.saveAndFlush(new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, NOW));

    // When / Then
    assertThat(userRepository.findByEmail("ana@exemplo.com"))
        .get()
        .extracting(User::getName)
        .isEqualTo("Ana Souza");
    assertThat(userRepository.existsByEmail("ana@exemplo.com")).isTrue();
    assertThat(userRepository.findByEmail("bruno@exemplo.com")).isEmpty();
    assertThat(userRepository.existsByEmail("bruno@exemplo.com")).isFalse();
  }

  @Test
  void shouldRejectInsert_whenEmailDiffersOnlyByCase() {
    // Given
    userRepository.saveAndFlush(new User("Ana Souza", "ana@exemplo.com", "hash", Role.USER, NOW));
    User duplicate = new User("Ana Outra", "ANA@Exemplo.com", "hash", Role.USER, NOW);

    // When / Then
    // Hibernate's SQLite dialect does not translate SQLITE_CONSTRAINT_UNIQUE, so Spring surfaces a
    // JpaSystemException rather than a DataIntegrityViolationException; the column name is stable.
    assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("users.email");
  }

  @Test
  void shouldRejectInsert_whenRoleIsOutsideTheEnum() {
    // When / Then
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "INSERT INTO users (public_id, name, email, password_hash, created_at, role)"
                        + " VALUES (?, 'Raiz', 'root@exemplo.com', 'hash', ?, 'ROOT')",
                    UUID.randomUUID().toString(),
                    Timestamp.from(NOW.toInstant())))
        .isInstanceOf(DataAccessException.class)
        .hasMessageContaining("CHECK constraint failed");
  }

  @Test
  void shouldDefaultRoleToUser_whenRoleIsOmitted() {
    // Given
    UUID publicId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO users (public_id, name, email, password_hash, created_at)"
            + " VALUES (?, 'Sem Papel', 'semrole@exemplo.com', 'hash', ?)",
        publicId.toString(),
        Timestamp.from(NOW.toInstant()));

    // When
    User reloaded = userRepository.findByPublicId(publicId).orElseThrow();

    // Then
    assertThat(reloaded.getRole()).isEqualTo(Role.USER);
    assertThat(reloaded.getCreatedAt()).isAtSameInstantAs(NOW);
  }
}
