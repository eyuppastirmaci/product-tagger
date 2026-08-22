package com.producttagger.backend.shared.outbox;

import com.producttagger.backend.shared.messaging.Messaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Publishes unpublished outbox rows to RabbitMQ. Delivery is at-least-once
 * (a crash between send and commit re-sends the batch), so consumers must be
 * idempotent.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outbox;
    private final RabbitTemplate rabbitTemplate;

    OutboxRelay(OutboxEventRepository outbox, RabbitTemplate rabbitTemplate) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    void publishPending() {
        List<OutboxEvent> pending = outbox.findTop50ByPublishedAtIsNullOrderByIdAsc();

        for (OutboxEvent event : pending) {
            rabbitTemplate.convertAndSend(Messaging.EXCHANGE, event.getEventType(), event.getPayload());

            event.markPublished();
        }

        if (!pending.isEmpty()) {
            log.debug("Published {} outbox event(s)", pending.size());
        }
    }
}
