package com.producttagger.backend.product.infrastructure.messaging;

import com.producttagger.backend.shared.messaging.Messaging;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue topology of the product pipeline. Rejected messages keep their original
 * routing key when dead-lettered, so each DLQ binds to the DLX with the same key
 * as its main queue.
 */
@Configuration
class ProductQueuesConfig {

    @Bean
    TopicExchange deadLetterExchange() {
        return new TopicExchange(Messaging.DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue uploadedQueue() {
        return QueueBuilder.durable(ProductMessaging.UPLOADED)
                .deadLetterExchange(Messaging.DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    Queue uploadedDlq() {
        return QueueBuilder.durable(ProductMessaging.UPLOADED_DLQ).build();
    }

    @Bean
    Binding uploadedBinding(Queue uploadedQueue, TopicExchange productTaggerExchange) {
        return BindingBuilder.bind(uploadedQueue).to(productTaggerExchange).with(ProductMessaging.UPLOADED);
    }

    @Bean
    Binding uploadedDlqBinding(Queue uploadedDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(uploadedDlq).to(deadLetterExchange).with(ProductMessaging.UPLOADED);
    }

    @Bean
    Queue readyForTaggingQueue() {
        return QueueBuilder.durable(ProductMessaging.READY_FOR_TAGGING)
                .deadLetterExchange(Messaging.DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    Queue readyForTaggingDlq() {
        return QueueBuilder.durable(ProductMessaging.READY_FOR_TAGGING_DLQ).build();
    }

    @Bean
    Binding readyForTaggingBinding(Queue readyForTaggingQueue, TopicExchange productTaggerExchange) {
        return BindingBuilder.bind(readyForTaggingQueue).to(productTaggerExchange)
                .with(ProductMessaging.READY_FOR_TAGGING);
    }

    @Bean
    Binding readyForTaggingDlqBinding(Queue readyForTaggingDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(readyForTaggingDlq).to(deadLetterExchange)
                .with(ProductMessaging.READY_FOR_TAGGING);
    }

    @Bean
    Queue approvedQueue() {
        return QueueBuilder.durable(ProductMessaging.APPROVED)
                .deadLetterExchange(Messaging.DEAD_LETTER_EXCHANGE)
                .build();
    }

    @Bean
    Queue approvedDlq() {
        return QueueBuilder.durable(ProductMessaging.APPROVED_DLQ).build();
    }

    @Bean
    Binding approvedBinding(Queue approvedQueue, TopicExchange productTaggerExchange) {
        return BindingBuilder.bind(approvedQueue).to(productTaggerExchange).with(ProductMessaging.APPROVED);
    }

    @Bean
    Binding approvedDlqBinding(Queue approvedDlq, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(approvedDlq).to(deadLetterExchange).with(ProductMessaging.APPROVED);
    }
}
