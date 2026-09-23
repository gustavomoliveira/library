package dev.gustavo.pblibrary.infrastructure.messaging;

import dev.gustavo.pblibrary.domain.loan.LoanReturnedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoanEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private LoanEventPublisher publisher;

    private final LoanReturnedEvent event =
            new LoanReturnedEvent(1L, 1L, LocalDate.now().minusDays(20), LocalDate.now());

    @Test
    void onLoanReturned_deveEnviarParaExchangeComRoutingKeyCorreta() {
        publisher.onLoanReturned(event);

        verify(rabbitTemplate).convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.LOAN_RETURNED_KEY, event);
    }

    @Test
    void onLoanReturned_quandoRabbitFalha_naoDevePropagarExcecao() {
        doThrow(new RuntimeException("broker indisponível"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> publisher.onLoanReturned(event)).doesNotThrowAnyException();
    }
}