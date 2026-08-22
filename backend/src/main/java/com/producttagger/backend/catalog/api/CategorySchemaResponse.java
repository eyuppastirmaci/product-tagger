package com.producttagger.backend.catalog.api;

import java.util.Map;

public record CategorySchemaResponse(
        String categoryCode,
        int version,
        Map<String, Object> schema) {
}
