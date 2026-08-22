package com.producttagger.backend.catalog.domain;

import java.util.Locale;

public enum AttributeType {
    ENUM,
    BOOLEAN,
    TEXT;

    /**
     * Schemas are seed-controlled, so an unknown type is a data error worth
     * failing loudly on, not a value to tolerate.
     */
    public static AttributeType from(String raw) {
        for (AttributeType type : values()) {
            if (type.name().equalsIgnoreCase(raw)) {
                return type;
            }
        }

        throw new IllegalStateException("Unknown attribute type '%s' in schema".formatted(raw));
    }

    public String jsonValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
