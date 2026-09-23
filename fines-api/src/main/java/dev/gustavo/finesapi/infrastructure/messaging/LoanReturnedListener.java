package dev.gustavo.finesapi.infrastructure.messaging;

import dev.gustavo.finesapi.domain.fine.FineRequestDTO;
import dev.gustavo.finesapi.domain.fine.FineResponseDTO;
import dev.gustavo.finesapi.domain.fine.FineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoanReturnedListener {

    private static final Logger log = LoggerFactory.getLogger(LoanReturnedListener.class);

    private final FineService fineService;

    public LoanReturnedListener(FineService fineService) {
        this.fineService = fineService;
    }

    @RabbitListener(queues = RabbitMQConfig.LOAN_RETURNED_QUEUE)
    public void onLoanReturned(LoanReturnedEvent event) {
        log.info("LoanReturnedEvent recebido para o empréstimo {}", event.loanId());

        FineRequestDTO request = new FineRequestDTO(
                event.loanId(),
                event.userId(),
                event.loanDate(),
                event.returnDate());

        Optional<FineResponseDTO> fine = fineService.createFine(request);

        if (fine.isPresent()) {
            log.info("Multa criada para o empréstimo {}: {} dias de atraso", event.loanId(), fine.get().daysLate());
        } else {
            log.info("Empréstimo {} devolvido sem atraso, nenhuma multa gerada", event.loanId());
        }
    }
}