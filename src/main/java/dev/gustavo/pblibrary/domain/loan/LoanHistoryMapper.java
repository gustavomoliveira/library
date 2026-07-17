package dev.gustavo.pblibrary.domain.loan;

public class LoanHistoryMapper {

    public static LoanHistoryResponseDTO toDTO(LoanHistory entity) {
        return new LoanHistoryResponseDTO(
                entity.getId(),
                entity.getLoan().getId(),
                entity.getEventType(),
                entity.getEventDate());
    }
}