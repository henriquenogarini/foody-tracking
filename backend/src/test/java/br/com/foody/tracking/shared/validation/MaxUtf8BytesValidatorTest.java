package br.com.foody.tracking.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.foody.tracking.modules.auth.dto.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MaxUtf8BytesValidatorTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    factory.close();
  }

  @Test
  void shouldAcceptValue_whenWithinByteLimit() {
    // When / Then
    assertThat(validator.validate(new Sample("é".repeat(36)))).isEmpty();
  }

  @Test
  void shouldRejectValue_whenCharactersFitButBytesExceedLimit() {
    // Given: 37 characters, 74 UTF-8 bytes
    Sample sample = new Sample("é".repeat(37));

    // When / Then
    assertThat(validator.validate(sample))
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactly("value");
  }

  @Test
  void shouldAcceptNull_whenValueIsAbsent() {
    // When / Then
    assertThat(validator.validate(new Sample(null))).isEmpty();
  }

  @Test
  void shouldRejectRegisterPassword_whenSeventyTwoAccentedCharactersExceedBcryptBytes() {
    // Given
    RegisterRequest request = new RegisterRequest("Ana Souza", "ana@exemplo.com", "ã".repeat(72));

    // When / Then
    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .containsExactly("password");
  }

  private record Sample(@MaxUtf8Bytes(72) String value) {}
}
