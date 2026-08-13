package dev.gustavo.finesapi.domain.fine;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FineResponseDTO(
        Long id,
        Long loanId,
        Long userId,
        int daysLate,
        BigDecimal amount,
        FineStatus status,
        LocalDateTime createdAt) {
}