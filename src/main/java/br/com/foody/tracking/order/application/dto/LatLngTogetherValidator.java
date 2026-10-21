package br.com.foody.tracking.order.application.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class LatLngTogetherValidator
    implements ConstraintValidator<LatLngTogether, AddressRequest> {

  // The violation is attached to the missing coordinate so the 400 names a field
  // (deliveryAddress.lat or deliveryAddress.lng) rather than the whole address object.
  @Override
  public boolean isValid(AddressRequest address, ConstraintValidatorContext context) {
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
