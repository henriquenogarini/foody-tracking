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

// @Size counts characters, but BCrypt limits bytes: it only accepts 72 bytes and Spring's encoder
// throws above it, while a 72-character accented password exceeds 72 UTF-8 bytes. Every password
// path (registration DTO, login pre-check, admin bootstrap) caps at BCRYPT_MAX_BYTES.
@Documented
@Constraint(validatedBy = MaxUtf8BytesValidator.class)
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface MaxUtf8Bytes {

  int BCRYPT_MAX_BYTES = 72;

  int value();

  String message() default "deve ter no máximo {value} bytes (caracteres acentuados contam mais)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
