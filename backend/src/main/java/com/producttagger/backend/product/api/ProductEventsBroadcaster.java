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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory SSE registry: one global subscriber list fed by
 * {@link ProductStatusChanged} events after the transaction commits. Every
 * product's updates travel over a single connection per browser, staying far
 * below the browser's per-origin connection limit.
 */
@Component
public class ProductEventsBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(ProductEventsBroadcaster.class);

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    // AFTER_COMMIT: a client refetching on this event must see the new DB state
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ProductStatusChanged event) {
        StatusPayload payload = new StatusPayload(
                event.productId(), event.status().name(), event.descriptionsReady());

        for (SseEmitter emitter : emitters) {
            send(emitter, payload);
        }
    }

    // Keeps idle connections alive through proxies and prunes dead ones
    @Scheduled(fixedDelay = 25_000)
    void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    private void send(SseEmitter emitter, StatusPayload payload) {
        try {
            emitter.send(SseEmitter.event().name("status").data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("Dropping dead SSE subscriber");
            emitters.remove(emitter);
        }
    }

    public record StatusPayload(UUID productId, String status, boolean descriptionsReady) {
    }
}
