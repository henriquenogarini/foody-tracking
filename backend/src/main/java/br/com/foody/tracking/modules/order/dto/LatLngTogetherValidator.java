package br.com.foody.tracking.modules.order.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class LatLngTogetherValidator
    implements ConstraintValidator<LatLngTogether, AddressRequestDTO> {

  @Override
  public boolean isValid(AddressRequestDTO address, ConstraintValidatorContext context) {
    if (address == null || (address.lat() == null) == (address.lng() == null)) {
      return true;
    }
    String missing = address.lat() == null ? "lat" : "lng";
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode(missing)
        .addConstraintViolation();
    return false;
  }
}
