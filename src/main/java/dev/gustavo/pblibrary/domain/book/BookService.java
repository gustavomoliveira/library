package dev.gustavo.pblibrary.domain.book;

import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public BookResponseDTO createBook(BookDTO dto) {
        Book book = BookMapper.toEntity(dto);
        return BookMapper.toDTO(repository.save(book));
    }

    public BookResponseDTO updateBook(Long id, BookDTO dto) {
        Optional<Book> existingBook = repository.findById(id);

        if (existingBook.isEmpty()) throw new ResourceNotFoundException("Book not found.");

        Book book = existingBook.get();
        book.setTitle(dto.title());
        book.setAuthor(dto.author());
        book.setIsbn(dto.isbn());

        return BookMapper.toDTO(repository.save(book));
    }

    public void deleteBook(Long id) {
        Optional<Book> existingBook = repository.findById(id);

        if (existingBook.isEmpty()) throw new ResourceNotFoundException("Book not found.");

        Book book = existingBook.get();
        repository.delete(book);
    }

    public BookResponseDTO findBookByIsbn(String isbn) {
        Optional<Book> existingBook = repository.findByIsbn(isbn);

        if (existingBook.isEmpty()) throw new ResourceNotFoundException("Book not found.");

        return BookMapper.toDTO(existingBook.get());
    }

    public List<BookResponseDTO> findBookByAuthor(String author) {
        List<Book> existingBooks = repository.findByAuthor(author);

        if (existingBooks.isEmpty()) throw new ResourceNotFoundException("No books found for the given author.");

        return existingBooks.stream().map(BookMapper::toDTO).toList();
    }

    public List<BookResponseDTO> findBookByTitle(String title) {
        List<Book> existingBooks = repository.findByTitle(title);

        if (existingBooks.isEmpty()) throw new ResourceNotFoundException("No books found for the given title.");

        return existingBooks.stream().map(BookMapper::toDTO).toList();
    }
}
