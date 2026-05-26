package dev.gustavo.pblibrary.domain.loan;

import java.time.LocalDate;

public record LoanResponseDTO(Long id, Long bookId, Long userId, LocalDate loanDate, LocalDate returnDate) {
}
