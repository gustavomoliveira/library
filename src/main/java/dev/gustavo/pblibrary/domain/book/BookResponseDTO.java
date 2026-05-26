package dev.gustavo.pblibrary.domain.book;

public record BookResponseDTO(
        Long id,
        String title,
        String author,
        String isbn,
        Integer totalCopies,
        Integer availableCopies) {
}
