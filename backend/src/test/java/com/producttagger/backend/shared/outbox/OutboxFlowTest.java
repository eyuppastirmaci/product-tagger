package com.producttagger.backend.shared.outbox;

import com.producttagger.backend.IntegrationTest;
import com.producttagger.backend.shared.messaging.Messaging;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class OutboxFlowTest extends IntegrationTest {

    private static final String TEST_ROUTING_KEY = "outbox.test";
    private static final String TEST_QUEUE = "outbox-test-queue";

    @Autowired
    private OutboxEventRepository outbox;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Test
    void relayPublishesPendingRowAndMarksIt() {
        // A throwaway queue so the test observes the message without touching
        // the real pipeline queues
        amqpAdmin.declareQueue(new Queue(TEST_QUEUE, false, false, true));
        amqpAdmin.declareBinding(bindTestQueue());

        OutboxEvent saved = outbox.save(
                OutboxEvent.of(TEST_ROUTING_KEY, UUID.randomUUID(), Map.of("productId", "test")));

        assertThat(saved.getPublishedAt()).isNull();

        // The scheduled relay runs every second; give it a few ticks
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(outbox.findById(saved.getId()).orElseThrow().getPublishedAt()).isNotNull());

        Object message = rabbitTemplate.receiveAndConvert(TEST_QUEUE, 5000);

        assertThat(message).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) message).get("productId")).isEqualTo("test");
    }

    private static Binding bindTestQueue() {
        return BindingBuilder.bind(new Queue(TEST_QUEUE))
                .to(new TopicExchange(Messaging.EXCHANGE))
                .with(TEST_ROUTING_KEY);
    }
}
