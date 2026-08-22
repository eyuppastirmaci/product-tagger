package com.producttagger.backend.product.infrastructure.ai;

import com.producttagger.backend.catalog.domain.AttributeDefinition;
import com.producttagger.backend.product.application.TaggingModelClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Map;

@Component
class SpringAiTaggingClient implements TaggingModelClient {

    private static final String CATEGORY_SYSTEM_PROMPT = """
            You are a product cataloging assistant. Look at the product photo and \
            pick the single best matching category. Never invent categories: answer \
            only with one of the offered codes, or "other" if none fits or you are \
            unsure. Report your confidence between 0.0 and 1.0.""";

    private static final String ATTRIBUTES_SYSTEM_PROMPT = """
            You extract product attributes from a photo. Follow the attribute \
            definitions strictly: use only the listed enum values, respect the \
            multi flag (single value vs. list), and list multiple values most \
            dominant first (e.g. main color before accent colors). Skip attributes \
            you cannot infer from the photo instead of guessing. For every returned \
            attribute report a confidence between 0.0 and 1.0.""";

    private final ChatClient chatClient;
    private final ModelOutputSanitizer sanitizer;
    private final AttributeSchemaPromptMapper schemaMapper;

    SpringAiTaggingClient(ChatClient.Builder chatClientBuilder,
                          ModelOutputSanitizer sanitizer,
                          AttributeSchemaPromptMapper schemaMapper) {
        this.chatClient = chatClientBuilder.build();
        this.sanitizer = sanitizer;
        this.schemaMapper = schemaMapper;
    }

    @Override
    public CategoryChoice pickCategory(byte[] image, List<CategoryOption> options) {
        BeanOutputConverter<CategoryChoiceResponse> converter =
                new BeanOutputConverter<>(CategoryChoiceResponse.class);

        StringBuilder prompt = new StringBuilder("Which category matches the product in the photo? Options:\n");
        for (CategoryOption option : options) {
            prompt.append("- ").append(option.code()).append(" (").append(option.name()).append(")\n");
        }
        prompt.append("- other (none of the above)\n\n").append(converter.getFormat());

        ChatResponse response = complete(prompt.toString(), CATEGORY_SYSTEM_PROMPT, image);

        CategoryChoiceResponse choice = converter.convert(sanitizer.clean(response));

        return new CategoryChoice(
                choice.category(),
                choice.confidence() == null ? 0.0 : choice.confidence(),
                response.getMetadata().getModel());
    }

    @Override
    public AttributeExtraction extractAttributes(byte[] image, List<AttributeDefinition> definitions) {
        BeanOutputConverter<AttributeExtractionResponse> converter =
                new BeanOutputConverter<>(AttributeExtractionResponse.class);

        String prompt = "Extract the product's attributes from the photo.\n\nAttribute definitions:\n"
                + schemaMapper.toPromptJson(definitions) + "\n\n" + converter.getFormat();

        ChatResponse response = complete(prompt, ATTRIBUTES_SYSTEM_PROMPT, image);

        AttributeExtractionResponse extraction = converter.convert(sanitizer.clean(response));

        return new AttributeExtraction(
                extraction.attributes() == null ? Map.of() : extraction.attributes(),
                extraction.confidences() == null ? Map.of() : extraction.confidences(),
                response.getMetadata().getModel());
    }

    private ChatResponse complete(String userPrompt, String systemPrompt, byte[] image) {
        return chatClient.prompt()
                .system(systemPrompt)
                .user(user -> user.text(userPrompt)
                        .media(MimeTypeUtils.IMAGE_JPEG, new ByteArrayResource(image)))
                .call()
                .chatResponse();
    }

    record CategoryChoiceResponse(String category, Double confidence) {
    }

    record AttributeExtractionResponse(Map<String, Object> attributes, Map<String, Double> confidences) {
    }
}
