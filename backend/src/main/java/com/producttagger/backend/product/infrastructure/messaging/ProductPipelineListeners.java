package com.producttagger.backend.product.infrastructure.messaging;

import com.producttagger.backend.product.application.DescriptionService;
import com.producttagger.backend.product.application.TaggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class ProductPipelineListeners {

    private static final Logger log = LoggerFactory.getLogger(ProductPipelineListeners.class);

    private final TaggingService taggingService;
    private final DescriptionService descriptionService;

    ProductPipelineListeners(TaggingService taggingService, DescriptionService descriptionService) {
        this.taggingService = taggingService;
        this.descriptionService = descriptionService;
    }

    @RabbitListener(queues = ProductMessaging.READY_FOR_TAGGING)
    void onReadyForTagging(ProductMessage message) {
        taggingService.tagProduct(UUID.fromString(message.productId()));
    }

    // Preprocessing is synchronous today; this consumer only acknowledges the
    // message so the queue does not grow unbounded
    @RabbitListener(queues = ProductMessaging.UPLOADED)
    void onUploaded(ProductMessage message) {
        log.debug("Product {} uploaded", message.productId());
    }

    @RabbitListener(queues = ProductMessaging.READY_FOR_TAGGING_DLQ)
    void onTaggingDeadLetter(ProductMessage message) {
        taggingService.markTaggingFailed(UUID.fromString(message.productId()));
    }

    @RabbitListener(queues = ProductMessaging.APPROVED)
    void onApproved(ProductMessage message) {
        descriptionService.generateFor(UUID.fromString(message.productId()));
    }

    // Descriptions are an addition to the approval, not a condition of it: the
    // product stays APPROVED, only the failure is logged
    @RabbitListener(queues = ProductMessaging.APPROVED_DLQ)
    void onDescriptionDeadLetter(ProductMessage message) {
        log.warn("Description generation permanently failed for product {}", message.productId());
    }

    record ProductMessage(String productId) {
    }
}
