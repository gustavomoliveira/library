package dev.gustavo.pblibrary.domain.loan;

import java.time.LocalDate;

public record LoanReturnedEvent(
        Long loanId,
        Long userId,
        LocalDate loanDate,
        LocalDate returnDate
) {
}
