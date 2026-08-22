package com.producttagger.backend.product.infrastructure.messaging;

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

    ProductPipelineListeners(TaggingService taggingService) {
        this.taggingService = taggingService;
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

    record ProductMessage(String productId) {
    }
}
