package br.com.foody.tracking.modules.order.dto;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

// Cross-field rule mirrored by the domain and by ck_orders_lat_lng: checking it here turns a
// half-filled pair into a 400 on the missing field instead of an exception deeper down.
@Documented
@Constraint(validatedBy = LatLngTogetherValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface LatLngTogether {

  String message() default "latitude e longitude devem ser informadas juntas";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
