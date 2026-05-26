package dev.gustavo.pblibrary.domain.book;

public class BookMapper {

    public static Book toEntity(BookRequestDTO dto) {
        return new Book(dto.title(), dto.author(), dto.isbn(), dto.totalCopies());
    }

    public static BookResponseDTO toDTO(Book entity) {
        return new BookResponseDTO(
                entity.getId(),
                entity.getTitle(),
                entity.getAuthor(),
                entity.getIsbn(),
                entity.getTotalCopies(),
                entity.getAvailableCopies());
    }
}
