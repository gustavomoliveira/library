package dev.gustavo.pblibrary.domain.book;

import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public BookResponseDTO createBook(BookRequestDTO dto) {
        Book book = BookMapper.toEntity(dto);
        return BookMapper.toDTO(repository.save(book));
    }

    public BookResponseDTO updateBook(Long id, BookRequestDTO dto) {
        Book book = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found."));
        book.setTitle(dto.title());
        book.setAuthor(dto.author());
        book.setIsbn(dto.isbn());
        return BookMapper.toDTO(repository.save(book));
    }

    public void deleteBook(Long id) {
        Book book = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found."));
        repository.delete(book);
    }

    public BookResponseDTO findBookById(Long id) {
        Book book = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book not found."));
        return BookMapper.toDTO(book);
    }

    public BookResponseDTO findBookByIsbn(String isbn) {
        Book book = repository.findByIsbn(isbn).orElseThrow(() -> new ResourceNotFoundException("Book not found."));
        return BookMapper.toDTO(book);
    }

    public List<BookResponseDTO> findBooksByAuthor(String author) {
        List<Book> existingBooks = repository.findByAuthor(author);
        return existingBooks.stream().map(BookMapper::toDTO).toList();
    }

    public List<BookResponseDTO> findBooksByTitle(String title) {
        List<Book> existingBooks = repository.findByTitle(title);
        return existingBooks.stream().map(BookMapper::toDTO).toList();
    }

    public List<BookResponseDTO> findAllBooks() {
        List<Book> existingBooks = repository.findAll();
        return existingBooks.stream().map(BookMapper::toDTO).toList();
    }

    public List<BookResponseDTO> findBooks(String title, String author) {
        if (title == null && author == null) return findAllBooks();
        if (title != null) return findBooksByTitle(title);
        return findBooksByAuthor(author);
    }
}
