package com.producttagger.backend.shared.outbox;

import com.producttagger.backend.shared.messaging.Messaging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publishes unpublished outbox rows to RabbitMQ. A row is marked published
 * only after the broker's publisher confirm, so delivery is at-least-once
 * (an unconfirmed or crashed batch is re-sent) and consumers must be
 * idempotent.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

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

        // Send the whole batch first; the confirms arrive while later sends
        // are still going out
        Map<OutboxEvent, CorrelationData> inFlight = new LinkedHashMap<>();

        for (OutboxEvent event : pending) {
            CorrelationData correlation = new CorrelationData(String.valueOf(event.getId()));

            rabbitTemplate.convertAndSend(Messaging.EXCHANGE, event.getEventType(), event.getPayload(), correlation);

            inFlight.put(event, correlation);
        }

        int published = 0;

        for (Map.Entry<OutboxEvent, CorrelationData> entry : inFlight.entrySet()) {
            if (confirmed(entry.getValue())) {
                entry.getKey().markPublished();

                published++;
            }
        }

        if (published > 0) {
            log.debug("Published {} outbox event(s)", published);
        }
    }

    /**
     * Waits for the broker ack; a nack, an unroutable return or a missing
     * confirm leaves the row unpublished so the next tick retries it.
     */
    private boolean confirmed(CorrelationData correlation) {
        try {
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(CONFIRM_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

            if (!confirm.ack()) {
                log.warn("Broker nacked outbox event {}; leaving it unpublished", correlation.getId());
                return false;
            }

            if (correlation.getReturned() != null) {
                log.warn("Outbox event {} was unroutable; leaving it unpublished", correlation.getId());
                return false;
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return false;
        } catch (ExecutionException | TimeoutException e) {
            log.warn("No publisher confirm for outbox event {}; leaving it unpublished", correlation.getId(), e);

            return false;
        }
    }
}
