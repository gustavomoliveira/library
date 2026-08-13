package dev.gustavo.pblibrary.infrastructure.client;

import java.time.LocalDate;

public record FineRequestDTO(Long loanId, Long userId, LocalDate loanDate, LocalDate returnDate) {
}