package dev.gustavo.pblibrary.infrastructure.messaging;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "library.events";
    public static final String LOAN_RETURNED_KEY = "loan.returned";

    @Bean
    public TopicExchange libraryEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
