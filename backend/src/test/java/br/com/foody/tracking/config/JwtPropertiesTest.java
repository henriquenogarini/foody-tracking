package br.com.foody.tracking.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    private static final String STRONG_SECRET =
            "4F7QtQJCWv6h/csoRdvkKJ8G81a2ugKuXnD3raXwk8+PcTOWFEw1IfGPrOJArc8x";

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
    void shouldAcceptProperties_whenSecretDecodesToAtLeast32Bytes() {
        // When
        Set<ConstraintViolation<JwtProperties>> violations =
                validator.validate(new JwtProperties(STRONG_SECRET, 7200));

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldRejectSecret_whenItDecodesToFewerThan32Bytes() {
        // Given
        String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

        // When / Then
        assertThat(validator.validate(new JwtProperties(shortSecret, 7200)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("secretAStrongBase64Key");
    }

    @Test
    void shouldRejectSecret_whenItIsNotBase64() {
        // When / Then
        assertThat(validator.validate(new JwtProperties("not base64 at all!!", 7200)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("secretAStrongBase64Key");
    }

    @Test
    void shouldRejectSecret_whenItIsBlank() {
        // When / Then
        assertThat(validator.validate(new JwtProperties(" ", 7200)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("secret");
    }

    @Test
    void shouldRejectExpiration_whenLongerThanTwoHoursOrNotPositive() {
        // When / Then
        assertThat(validator.validate(new JwtProperties(STRONG_SECRET, 7201)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("expirationSeconds");
        assertThat(validator.validate(new JwtProperties(STRONG_SECRET, 0)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("expirationSeconds");
    }
}
