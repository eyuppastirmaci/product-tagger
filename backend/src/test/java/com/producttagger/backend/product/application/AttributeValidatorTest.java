package com.producttagger.backend.product.application;

import com.producttagger.backend.catalog.domain.AttributeDefinition;
import com.producttagger.backend.catalog.domain.AttributeType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AttributeValidatorTest {

    private static final AttributeDefinition COLOR_MULTI = new AttributeDefinition(
            "color", AttributeType.ENUM, true, true, List.of("black", "white"));

    private static final AttributeDefinition PATTERN_SINGLE = new AttributeDefinition(
            "pattern", AttributeType.ENUM, true, false, List.of("solid", "striped"));

    private static final AttributeDefinition DISTRESSED = new AttributeDefinition(
            "distressed", AttributeType.BOOLEAN, false, false, List.of());

    private static final AttributeDefinition NOTE = new AttributeDefinition(
            "note", AttributeType.TEXT, false, false, List.of());

    private final AttributeValidator validator = new AttributeValidator();

    @Test
    void acceptsValidValues() {
        Map<String, Object> attributes = Map.of(
                "color", List.of("black", "white"),
                "pattern", "solid",
                "distressed", true,
                "note", "some text");

        assertThatCode(() -> validator.validate(attributes,
                List.of(COLOR_MULTI, PATTERN_SINGLE, DISTRESSED, NOTE)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownAttribute() {
        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(Map.of("bogus", "x"), List.of(NOTE)))
                .withMessageContaining("unknown attribute 'bogus'");
    }

    @Test
    void rejectsMissingRequiredAttribute() {
        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(Map.of(), List.of(PATTERN_SINGLE)))
                .withMessageContaining("'pattern' is required");
    }

    @Test
    void rejectsEnumValueOutsideAllowedSet() {
        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(Map.of("pattern", "plaid"), List.of(PATTERN_SINGLE)))
                .withMessageContaining("invalid value 'plaid'");
    }

    @Test
    void multiEnumRequiresListAndSingleEnumRejectsList() {
        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(
                        Map.of("color", "black", "pattern", "solid"), List.of(COLOR_MULTI, PATTERN_SINGLE)))
                .withMessageContaining("'color' must be a list");

        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(
                        Map.of("color", List.of("black"), "pattern", List.of("solid")),
                        List.of(COLOR_MULTI, PATTERN_SINGLE)))
                .withMessageContaining("'pattern' must be a single value");
    }

    @Test
    void requiredMultiEnumRejectsEmptyList() {
        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(
                        Map.of("color", List.of(), "pattern", "solid"), List.of(COLOR_MULTI, PATTERN_SINGLE)))
                .withMessageContaining("'color' must not be empty");
    }

    @Test
    void rejectsWrongScalarTypes() {
        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(Map.of("distressed", "yes"), List.of(DISTRESSED)))
                .withMessageContaining("'distressed' must be a boolean");

        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(Map.of("note", 42), List.of(NOTE)))
                .withMessageContaining("'note' must be a string");
    }

    @Test
    void collectsEveryViolationInOneException() {
        assertThatExceptionOfType(InvalidAttributesException.class)
                .isThrownBy(() -> validator.validate(
                        Map.of("bogus", "x", "pattern", "plaid"), List.of(COLOR_MULTI, PATTERN_SINGLE)))
                .withMessageContaining("unknown attribute 'bogus'")
                .withMessageContaining("'color' is required")
                .withMessageContaining("invalid value 'plaid'");
    }
}
