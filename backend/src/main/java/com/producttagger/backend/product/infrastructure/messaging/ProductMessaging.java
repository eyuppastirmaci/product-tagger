package com.producttagger.backend.product.infrastructure.messaging;

/**
 * Names shared between the outbox writer and the RabbitMQ topology.
 * Event types double as routing keys.
 */
public final class ProductMessaging {

    public static final String UPLOADED = "product.uploaded";

    public static final String UPLOADED_DLQ = UPLOADED + ".dlq";

    public static final String READY_FOR_TAGGING = "product.ready_for_tagging";

    public static final String READY_FOR_TAGGING_DLQ = READY_FOR_TAGGING + ".dlq";

    private ProductMessaging() {
    }
}
