package dev.gustavo.pblibrary.domain.book;

public class BookMapper {

    public static Book toEntity(BookDTO dto) {
        return new Book(dto.title(), dto.author(), dto.isbn());
    }

    public static BookResponseDTO toDTO(Book entity) {
        return new BookResponseDTO(entity.getId(), entity.getTitle(), entity.getAuthor(), entity.getIsbn());
    }
}
