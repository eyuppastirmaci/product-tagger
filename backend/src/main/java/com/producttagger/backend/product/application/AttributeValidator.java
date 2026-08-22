package com.producttagger.backend.product.application;

import com.producttagger.backend.catalog.domain.AttributeDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Validates submitted attribute values against a category's schema; all
 * violations are collected and reported in a single exception.
 */
@Component
public class AttributeValidator {

    public void validate(Map<String, Object> attributes, List<AttributeDefinition> definitions) {
        List<String> errors = new ArrayList<>();

        Map<String, AttributeDefinition> byKey = definitions.stream()
                .collect(Collectors.toMap(AttributeDefinition::key, Function.identity()));

        for (String key : attributes.keySet()) {
            if (!byKey.containsKey(key)) {
                errors.add("unknown attribute '%s'".formatted(key));
            }
        }

        for (AttributeDefinition definition : definitions) {
            Object value = attributes.get(definition.key());

            if (value == null) {
                if (definition.required()) {
                    errors.add("attribute '%s' is required".formatted(definition.key()));
                }
                continue;
            }

            validateValue(definition, value, errors);
        }

        if (!errors.isEmpty()) {
            throw new InvalidAttributesException(String.join("; ", errors));
        }
    }

    // Exhaustive switch: a new AttributeType without a rule is a compile error
    private void validateValue(AttributeDefinition definition, Object value, List<String> errors) {
        switch (definition.type()) {
            case ENUM -> validateEnum(definition, value, errors);
            case BOOLEAN -> {
                if (!(value instanceof Boolean)) {
                    errors.add("attribute '%s' must be a boolean".formatted(definition.key()));
                }
            }
            case TEXT -> {
                if (!(value instanceof String)) {
                    errors.add("attribute '%s' must be a string".formatted(definition.key()));
                }
            }
        }
    }

    private void validateEnum(AttributeDefinition definition, Object value, List<String> errors) {
        if (definition.multi()) {
            if (!(value instanceof List<?> values)) {
                errors.add("attribute '%s' must be a list".formatted(definition.key()));
                return;
            }

            if (values.isEmpty() && definition.required()) {
                errors.add("attribute '%s' must not be empty".formatted(definition.key()));
                return;
            }

            for (Object entry : values) {
                validateEnumValue(definition, entry, errors);
            }
        } else {
            if (value instanceof List) {
                errors.add("attribute '%s' must be a single value, not a list".formatted(definition.key()));
                return;
            }

            validateEnumValue(definition, value, errors);
        }
    }

    private void validateEnumValue(AttributeDefinition definition, Object value, List<String> errors) {
        if (!(value instanceof String string) || !definition.values().contains(string)) {
            errors.add("attribute '%s' has invalid value '%s' (allowed: %s)"
                    .formatted(definition.key(), value, String.join(", ", definition.values())));
        }
    }
}
