package com.producttagger.backend.product.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.producttagger.backend.product.application.DescriptionModelClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class SpringAiDescriptionClient implements DescriptionModelClient {

    private static final String SYSTEM_PROMPT = """
            You write concise e-commerce product descriptions. Use ONLY the given \
            category and attributes; never invent details that are not listed \
            (no sizes, prices, materials or features). Write 2-3 natural sentences \
            per language in a friendly marketing tone, no bullet points.""";

    private final ChatClient chatClient;

    private final ModelOutputSanitizer sanitizer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    SpringAiDescriptionClient(ChatClient.Builder chatClientBuilder, ModelOutputSanitizer sanitizer) {
        this.chatClient = chatClientBuilder.build();
        this.sanitizer = sanitizer;
    }

    @Override
    public Descriptions generate(String categoryNameEn, String categoryNameTr, Map<String, Object> attributes) {
        BeanOutputConverter<DescriptionsResponse> converter =
                new BeanOutputConverter<>(DescriptionsResponse.class);

        String prompt = """
                Category: %s (Turkish: %s)
                Attributes: %s

                Write the product description in Turkish (descriptionTr) and in English (descriptionEn).

                %s""".formatted(categoryNameEn, categoryNameTr, toJson(attributes), converter.getFormat());

        ChatResponse response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .chatResponse();

        DescriptionsResponse descriptions = converter.convert(sanitizer.clean(response));

        return new Descriptions(descriptions.descriptionTr(), descriptions.descriptionEn());
    }

    private String toJson(Map<String, Object> attributes) {
        try {
            return objectMapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize attributes", e);
        }
    }

    record DescriptionsResponse(String descriptionTr, String descriptionEn) {
    }
}
