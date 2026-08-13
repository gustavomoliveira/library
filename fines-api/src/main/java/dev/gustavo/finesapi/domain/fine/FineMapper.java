package dev.gustavo.finesapi.domain.fine;

public class FineMapper {

    public static FineResponseDTO toDTO(Fine entity) {
        return new FineResponseDTO(
                entity.getId(),
                entity.getLoanId(),
                entity.getUserId(),
                entity.getDaysLate(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getCreatedAt());
    }
}