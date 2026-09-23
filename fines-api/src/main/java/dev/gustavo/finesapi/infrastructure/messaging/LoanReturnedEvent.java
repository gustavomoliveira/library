package dev.gustavo.finesapi.infrastructure.messaging;

import java.time.LocalDate;

public record LoanReturnedEvent(Long loanId, Long userId, LocalDate loanDate, LocalDate returnDate) {
}