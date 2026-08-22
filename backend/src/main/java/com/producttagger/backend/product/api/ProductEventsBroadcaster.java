package com.producttagger.backend.product.api;

import com.producttagger.backend.product.domain.ProductStatusChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory SSE registry: one emitter list per product, fed by
 * {@link ProductStatusChanged} events after the transaction commits.
 */
@Component
public class ProductEventsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ProductEventsBroadcaster.class);

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID productId, StatusPayload initial) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitters.computeIfAbsent(productId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(productId, emitter));
        emitter.onTimeout(() -> remove(productId, emitter));
        emitter.onError(e -> remove(productId, emitter));

        // Immediate snapshot so the client never renders without a known state
        send(productId, emitter, initial);

        return emitter;
    }

    // AFTER_COMMIT: a client refetching on this event must see the new DB state
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ProductStatusChanged event) {
        List<SseEmitter> subscribers = emitters.get(event.productId());

        if (subscribers == null) {
            return;
        }

        StatusPayload payload = new StatusPayload(event.status().name(), event.descriptionsReady());

        for (SseEmitter emitter : subscribers) {
            send(event.productId(), emitter, payload);
        }
    }

    // Keeps idle connections alive through proxies and prunes dead ones
    @Scheduled(fixedDelay = 25_000)
    void heartbeat() {
        emitters.forEach((productId, subscribers) -> subscribers.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                remove(productId, emitter);
            }
        }));
    }

    private void send(UUID productId, SseEmitter emitter, StatusPayload payload) {
        try {
            emitter.send(SseEmitter.event().name("status").data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("Dropping dead SSE subscriber of product {}", productId);
            remove(productId, emitter);
        }
    }

    private void remove(UUID productId, SseEmitter emitter) {
        List<SseEmitter> subscribers = emitters.get(productId);

        if (subscribers != null) {
            subscribers.remove(emitter);

            if (subscribers.isEmpty()) {
                emitters.remove(productId, subscribers);
            }
        }
    }

    public record StatusPayload(String status, boolean descriptionsReady) {
    }
}
