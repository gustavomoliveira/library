package dev.gustavo.finesapi.domain.fine;

import java.time.LocalDate;

public record FineRequestDTO(Long loanId, Long userId, LocalDate loanDate, LocalDate returnDate) {
}