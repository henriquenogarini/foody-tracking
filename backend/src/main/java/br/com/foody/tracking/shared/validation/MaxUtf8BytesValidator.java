package br.com.foody.tracking.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public final class MaxUtf8BytesValidator
    implements ConstraintValidator<MaxUtf8Bytes, CharSequence> {

  private int maxBytes;

  @Override
  public void initialize(MaxUtf8Bytes constraint) {
    this.maxBytes = constraint.value();
  }

  // Null is valid by Bean Validation convention; @NotBlank owns presence.
  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || value.toString().getBytes(StandardCharsets.UTF_8).length <= maxBytes;
  }
}
