package com.producttagger.backend.product.infrastructure.messaging;

import com.producttagger.backend.product.domain.ProductReadyForTagging;
import com.producttagger.backend.product.domain.ProductUploaded;
import com.producttagger.backend.shared.outbox.OutboxEvent;
import com.producttagger.backend.shared.outbox.OutboxEventRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Writes product domain events to the outbox table in the same transaction as
 * the aggregate change. Must stay a plain {@code @EventListener};
 * {@code AFTER_COMMIT} would break that atomicity.
 */
@Component
class ProductEventsOutboxWriter {

    private final OutboxEventRepository outbox;

    ProductEventsOutboxWriter(OutboxEventRepository outbox) {
        this.outbox = outbox;
    }

    @EventListener
    void on(ProductUploaded event) {
        outbox.save(OutboxEvent.of(ProductMessaging.UPLOADED, event.productId(), payloadFor(event.productId())));
    }

    @EventListener
    void on(ProductReadyForTagging event) {
        outbox.save(OutboxEvent.of(ProductMessaging.READY_FOR_TAGGING, event.productId(), payloadFor(event.productId())));
    }

    private Map<String, Object> payloadFor(UUID productId) {
        return Map.of("productId", productId.toString());
    }
}
