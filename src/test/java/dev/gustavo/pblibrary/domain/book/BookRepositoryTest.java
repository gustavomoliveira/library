package dev.gustavo.pblibrary.domain.book;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository repository;

    @Test
    void findByIsbn_quandoExiste_deveRetornarOLivro() {
        repository.save(new Book("Effective Java", "Joshua Bloch", "978-0134685991", 3));

        Optional<Book> result = repository.findByIsbn("978-0134685991");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Effective Java");
    }

    @Test
    void findByIsbn_quandoNaoExiste_deveRetornarVazio() {
        Optional<Book> result = repository.findByIsbn("000-inexistente");

        assertThat(result).isEmpty();
    }

    @Test
    void findByAuthor_deveRetornarLivrosDoAutor() {
        repository.save(new Book("Effective Java", "Joshua Bloch", "978-0134685991", 3));
        repository.save(new Book("Java Concurrency in Practice", "Joshua Bloch", "978-0321349606", 2));

        List<Book> result = repository.findByAuthor("Joshua Bloch");

        assertThat(result).hasSize(2);
    }

    @Test
    void findByTitle_deveRetornarLivrosComOTitulo() {
        repository.save(new Book("Clean Code", "Robert C. Martin", "978-0132350884", 5));

        List<Book> result = repository.findByTitle("Clean Code");

        assertThat(result).hasSize(1);
    }
}
