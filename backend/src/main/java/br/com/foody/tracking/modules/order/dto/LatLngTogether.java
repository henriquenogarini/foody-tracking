package br.com.foody.tracking.modules.order.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = LatLngTogetherValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface LatLngTogether {

    String message() default "latitude e longitude devem ser informadas juntas";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
