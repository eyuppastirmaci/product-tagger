package com.producttagger.backend.product.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.producttagger.backend.catalog.domain.AttributeDefinition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders attribute definitions as the compact JSON the model receives;
 * display labels never reach the prompt.
 */
@Component
class AttributeSchemaPromptMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    String toPromptJson(List<AttributeDefinition> definitions) {
        try {
            return objectMapper.writeValueAsString(definitions.stream().map(this::compact).toList());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize attribute definitions", e);
        }
    }

    private Map<String, Object> compact(AttributeDefinition definition) {
        Map<String, Object> compact = new LinkedHashMap<>();

        compact.put("key", definition.key());
        compact.put("type", definition.type().jsonValue());
        compact.put("required", definition.required());

        if (definition.multi()) {
            compact.put("multi", true);
        }

        if (!definition.values().isEmpty()) {
            compact.put("values", definition.values());
        }

        return compact;
    }
}
