package dev.gustavo.pblibrary.domain.book;

import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookService service;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book("Clean Code", "Robert C. Martin", "978-0132350884", 5);
        book.setId(1L);
    }

    @Test
    void createBook_deveSalvarERetornarDTO() {
        when(repository.save(any(Book.class))).thenReturn(book);

        BookRequestDTO dto = new BookRequestDTO("Clean Code", "Robert C. Martin", "978-0132350884", 5);
        BookResponseDTO response = service.createBook(dto);

        assertThat(response.title()).isEqualTo("Clean Code");
        assertThat(response.availableCopies()).isEqualTo(5);
        verify(repository).save(any(Book.class));
    }

    @Test
    void updateBook_quandoExiste_deveAtualizarERetornarDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(book));
        when(repository.save(any(Book.class))).thenReturn(book);

        BookRequestDTO dto = new BookRequestDTO("Clean Code (2nd Edition)", "Robert C. Martin", "978-0132350884", 5);
        BookResponseDTO response = service.updateBook(1L, dto);

        assertThat(response.title()).isEqualTo("Clean Code (2nd Edition)");
        verify(repository).save(book);
    }

    @Test
    void updateBook_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        BookRequestDTO dto = new BookRequestDTO("Titulo", "Autor", "0000000000000", 1);

        assertThatThrownBy(() -> service.updateBook(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book not found.");
    }

    @Test
    void deleteBook_quandoExiste_deveDeletar() {
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        service.deleteBook(1L);

        verify(repository).delete(book);
    }

    @Test
    void deleteBook_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteBook(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findBookById_quandoExiste_deveRetornarDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(book));

        BookResponseDTO response = service.findBookById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void findBookById_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findBookById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findBookByIsbn_quandoExiste_deveRetornarDTO() {
        when(repository.findByIsbn("978-0132350884")).thenReturn(Optional.of(book));

        BookResponseDTO response = service.findBookByIsbn("978-0132350884");

        assertThat(response.isbn()).isEqualTo("978-0132350884");
    }

    @Test
    void findBookByIsbn_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findByIsbn("000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findBookByIsbn("000"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findBooksByAuthor_deveRetornarListaDeDTOs() {
        when(repository.findByAuthor("Robert C. Martin")).thenReturn(List.of(book));

        List<BookResponseDTO> response = service.findBooksByAuthor("Robert C. Martin");

        assertThat(response).hasSize(1);
    }

    @Test
    void findBooksByTitle_deveRetornarListaDeDTOs() {
        when(repository.findByTitle("Clean Code")).thenReturn(List.of(book));

        List<BookResponseDTO> response = service.findBooksByTitle("Clean Code");

        assertThat(response).hasSize(1);
    }

    @Test
    void findAllBooks_deveRetornarTodosOsLivros() {
        when(repository.findAll()).thenReturn(List.of(book));

        List<BookResponseDTO> response = service.findAllBooks();

        assertThat(response).hasSize(1);
    }

    @Test
    void findBooks_semParametros_deveChamarFindAllBooks() {
        when(repository.findAll()).thenReturn(List.of(book));

        List<BookResponseDTO> response = service.findBooks(null, null);

        assertThat(response).hasSize(1);
        verify(repository).findAll();
        verify(repository, never()).findByTitle(any());
        verify(repository, never()).findByAuthor(any());
    }

    @Test
    void findBooks_comTitulo_deveChamarFindByTitle() {
        when(repository.findByTitle("Clean Code")).thenReturn(List.of(book));

        service.findBooks("Clean Code", null);

        verify(repository).findByTitle("Clean Code");
        verify(repository, never()).findByAuthor(any());
    }

    @Test
    void findBooks_apenasComAutor_deveChamarFindByAuthor() {
        when(repository.findByAuthor("Robert C. Martin")).thenReturn(List.of(book));

        service.findBooks(null, "Robert C. Martin");

        verify(repository).findByAuthor("Robert C. Martin");
    }
}
