package br.com.foody.tracking.shared.web;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.foody.tracking.shared.exception.model.ApiErrorCode;
import br.com.foody.tracking.shared.exception.type.InvalidSortException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class SortWhitelistTest {

  private static final Set<String> ALLOWED = Set.of("createdAt", "name");

  @Test
  void shouldAccept_whenEverySortPropertyIsAllowed() {
    // Given
    Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("name"));

    // When / Then
    assertThatCode(() -> SortWhitelist.requireAllowed(sort, ALLOWED)).doesNotThrowAnyException();
  }

  @Test
  void shouldAccept_whenSortIsUnsorted() {
    // When / Then
    assertThatCode(() -> SortWhitelist.requireAllowed(Sort.unsorted(), ALLOWED))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectWithPropertyName_whenAnySortPropertyIsNotAllowed() {
    // Given
    Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("owner.passwordHash"));

    // When / Then
    assertThatThrownBy(() -> SortWhitelist.requireAllowed(sort, ALLOWED))
        .isInstanceOf(InvalidSortException.class)
        .hasMessage("Um ou mais campos são inválidos.")
        .extracting("property", "code")
        .containsExactly("owner.passwordHash", ApiErrorCode.VALIDATION_ERROR);
  }
}
