package com.producttagger.backend.shared.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MessagingConfig {

    // Declared on startup by Spring AMQP's admin; queues bind to it per context
    @Bean
    TopicExchange productTaggerExchange() {
        return new TopicExchange(Messaging.EXCHANGE, true, false);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
