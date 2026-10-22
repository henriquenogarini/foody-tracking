package br.com.foody.tracking.shared.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

// @Size counts characters, but some limits are in bytes: a 72-character accented string exceeds
// 72 UTF-8 bytes.
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface MaxUtf8Bytes {

  int value();

  String message() default "deve ter no máximo {value} bytes (caracteres acentuados contam mais)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
