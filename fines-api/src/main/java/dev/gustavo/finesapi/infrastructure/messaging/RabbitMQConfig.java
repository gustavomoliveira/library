package dev.gustavo.finesapi.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "library.events";
    public static final String LOAN_RETURNED_KEY = "loan.returned";
    public static final String LOAN_RETURNED_QUEUE = "fines.loan-returned";
    public static final String LOAN_RETURNED_DLQ = "fines.loan-returned.dlq";

    @Bean
    public TopicExchange libraryEventsExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue loanReturnedQueue() {
        return QueueBuilder.durable(LOAN_RETURNED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(LOAN_RETURNED_DLQ)
                .build();
    }

    @Bean
    public Queue loanReturnedDlq() {
        return QueueBuilder.durable(LOAN_RETURNED_DLQ).build();
    }

    @Bean
    public Binding loanReturnedBinding() {
        return BindingBuilder.bind(loanReturnedQueue())
                .to(libraryEventsExchange())
                .with(LOAN_RETURNED_KEY);
    }
}