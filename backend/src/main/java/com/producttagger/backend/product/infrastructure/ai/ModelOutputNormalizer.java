package com.producttagger.backend.product.infrastructure.ai;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enforces the bounds the prompts merely request: confidences are clamped
 * into [0,1] and titles into their length budget. A prompt instruction is not
 * validation, so nothing model-reported is stored as-is.
 */
@Component
class ModelOutputNormalizer {

    static final int MAX_TITLE_LENGTH = 60;

    double confidence(Double raw) {
        if (raw == null || raw.isNaN()) {
            return 0.0;
        }

        return Math.clamp(raw, 0.0, 1.0);
    }

    Map<String, Double> confidences(Map<String, Double> raw) {
        if (raw == null) {
            return Map.of();
        }

        Map<String, Double> clamped = new LinkedHashMap<>();

        raw.forEach((key, value) -> {
            if (key != null) {
                clamped.put(key, confidence(value));
            }
        });

        return clamped;
    }

    String title(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String title = raw.trim();

        if (title.length() > MAX_TITLE_LENGTH) {
            // Cut on a word boundary unless that would lose most of the budget
            int lastSpace = title.lastIndexOf(' ', MAX_TITLE_LENGTH);

            title = title.substring(0, lastSpace > MAX_TITLE_LENGTH / 2 ? lastSpace : MAX_TITLE_LENGTH).trim();
        }

        String stripped = stripTrailingPunctuation(title);

        return stripped.isBlank() ? null : stripped;
    }

    private static String stripTrailingPunctuation(String title) {
        int end = title.length();

        while (end > 0 && isTrailingPunctuation(title.charAt(end - 1))) {
            end--;
        }

        return title.substring(0, end).trim();
    }

    private static boolean isTrailingPunctuation(char c) {
        return c == '.' || c == ',' || c == ';' || c == ':' || c == '!' || c == '?';
    }
}
