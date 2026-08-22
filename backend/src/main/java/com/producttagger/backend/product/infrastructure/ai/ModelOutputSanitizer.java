package com.producttagger.backend.product.infrastructure.ai;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

/**
 * Safety net for reasoning models: strips a leaked <think> block and rejects
 * empty generations before any JSON parsing.
 */
@Component
class ModelOutputSanitizer {

    String clean(ChatResponse response) {
        String text = response.getResult() == null ? null : response.getResult().getOutput().getText();

        // No text at all means a failed generation; fail loudly so retry/DLQ kicks in
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Model returned an empty response");
        }

        return text.replaceAll("(?s)<think>.*?</think>", "").trim();
    }
}
