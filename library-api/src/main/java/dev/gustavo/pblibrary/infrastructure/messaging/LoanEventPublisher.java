package dev.gustavo.pblibrary.infrastructure.messaging;

import dev.gustavo.pblibrary.domain.loan.LoanReturnedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class LoanEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoanEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public LoanEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoanReturned(LoanReturnedEvent event) {
        log.info("Publicando LoanReturnedEvent para o empréstimo {}", event.loanId());
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.LOAN_RETURNED_KEY, event);
        } catch (Exception e) {
            log.error("Falha ao publicar LoanReturnedEvent para o empréstimo {}", event.loanId(), e);
        }
    }
}