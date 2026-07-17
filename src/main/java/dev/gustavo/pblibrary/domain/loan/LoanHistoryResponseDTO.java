package dev.gustavo.pblibrary.domain.loan;

import java.time.LocalDateTime;

public record LoanHistoryResponseDTO(Long id, Long loanId, LoanEventType eventType, LocalDateTime eventDate) {
}