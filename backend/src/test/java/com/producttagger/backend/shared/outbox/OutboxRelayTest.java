package com.producttagger.backend.shared.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OutboxRelay relay = new OutboxRelay(outbox, rabbitTemplate);

    @Test
    void ackedEventIsMarkedPublished() {
        OutboxEvent event = pendingEvent();

        stubBrokerResponse(correlation ->
                correlation.getFuture().complete(new CorrelationData.Confirm(true, null)));

        relay.publishPending();

        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    void nackedEventStaysUnpublished() {
        OutboxEvent event = pendingEvent();

        stubBrokerResponse(correlation ->
                correlation.getFuture().complete(new CorrelationData.Confirm(false, "queue overloaded")));

        relay.publishPending();

        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void unroutableEventStaysUnpublished() {
        OutboxEvent event = pendingEvent();

        stubBrokerResponse(correlation -> {
            // basic.return lands before the ack for an unroutable message
            correlation.setReturned(new ReturnedMessage(
                    new Message(new byte[0]), 312, "NO_ROUTE", "product-tagger", "bogus.key"));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        relay.publishPending();

        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void failedConfirmStaysUnpublished() {
        OutboxEvent event = pendingEvent();

        stubBrokerResponse(correlation ->
                correlation.getFuture().completeExceptionally(new IllegalStateException("channel closed")));

        relay.publishPending();

        assertThat(event.getPublishedAt()).isNull();
    }

    private OutboxEvent pendingEvent() {
        OutboxEvent event = OutboxEvent.of("product.uploaded", UUID.randomUUID(), Map.of("k", "v"));

        when(outbox.findTop50ByPublishedAtIsNullOrderByIdAsc()).thenReturn(List.of(event));

        return event;
    }

    private void stubBrokerResponse(Consumer<CorrelationData> broker) {
        doAnswer(invocation -> {
            broker.accept(invocation.getArgument(3));

            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    }
}
