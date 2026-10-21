package br.com.foody.tracking.order.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LatLngTogetherValidatorTest {

  private static final String MESSAGE = "latitude e longitude devem ser informadas juntas";

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

  private static CreateOrderRequest request(Double lat, Double lng) {
    return new CreateOrderRequest(
        new CustomerRequest("José da Silva", "+5511987521144", "jose@exemplo.com"),
        List.of(new OrderItemRequest("Pizza calabresa G", 1, new BigDecimal("59.90"))),
        new AddressRequest(
            "Rua José Neves",
            "590",
            "sala 03",
            "Vila São Paulo",
            "São Paulo",
            "SP",
            "04650-140",
            lat,
            lng),
        "sem cebola");
  }

  private static List<Tuple> pathsAndMessages(
      Set<ConstraintViolation<CreateOrderRequest>> violations) {
    return violations.stream()
        .map(v -> tuple(v.getPropertyPath().toString(), v.getMessage()))
        .toList();
  }

  @Test
  void shouldAccept_whenBothCoordinatesArePresent() {
    // When / Then
    assertThat(validator.validate(request(-23.6578, -46.6743))).isEmpty();
  }

  @Test
  void shouldAccept_whenBothCoordinatesAreAbsent() {
    // When / Then
    assertThat(validator.validate(request(null, null))).isEmpty();
  }

  @Test
  void shouldReportLng_whenOnlyLatIsPresent() {
    // When
    Set<ConstraintViolation<CreateOrderRequest>> violations =
        validator.validate(request(-23.6578, null));

    // Then: nested path is what the GlobalExceptionHandler exposes as errors[].field
    assertThat(pathsAndMessages(violations)).containsExactly(tuple("deliveryAddress.lng", MESSAGE));
  }

  @Test
  void shouldReportLat_whenOnlyLngIsPresent() {
    // When
    Set<ConstraintViolation<CreateOrderRequest>> violations =
        validator.validate(request(null, -46.6743));

    // Then
    assertThat(pathsAndMessages(violations)).containsExactly(tuple("deliveryAddress.lat", MESSAGE));
  }

  @Test
  void shouldAccept_whenAddressIsNull() {
    // When / Then
    assertThat(new LatLngTogetherValidator().isValid(null, null)).isTrue();
  }
}
