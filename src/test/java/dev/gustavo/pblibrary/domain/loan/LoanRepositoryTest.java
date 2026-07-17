package dev.gustavo.pblibrary.domain.loan;

import dev.gustavo.pblibrary.domain.book.Book;
import dev.gustavo.pblibrary.domain.book.BookRepository;
import dev.gustavo.pblibrary.domain.user.User;
import dev.gustavo.pblibrary.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanRepositoryTest {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private Book book;
    private User user;

    @BeforeEach
    void setUp() {
        book = bookRepository.save(new Book("Clean Code", "Robert C. Martin", "978-0132350884", 5));
        user = userRepository.save(new User("Gustavo Oliveira", "gustavo@email.com"));
    }

    @Test
    void findByReturnDateIsNull_deveRetornarApenasEmprestimosAtivos() {
        loanRepository.save(new Loan(book, user, LocalDate.now(), null));

        List<Loan> result = loanRepository.findByReturnDateIsNull();

        assertThat(result).hasSize(1);
    }

    @Test
    void findByReturnDateIsNull_naoDeveRetornarEmprestimosJaDevolvidos() {
        loanRepository.save(new Loan(book, user, LocalDate.now().minusDays(5), LocalDate.now()));

        List<Loan> result = loanRepository.findByReturnDateIsNull();

        assertThat(result).isEmpty();
    }

    @Test
    void findByUser_Id_deveRetornarEmprestimosDoUsuario() {
        loanRepository.save(new Loan(book, user, LocalDate.now(), null));

        List<Loan> result = loanRepository.findByUser_Id(user.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void findByBook_Id_deveRetornarEmprestimosDoLivro() {
        loanRepository.save(new Loan(book, user, LocalDate.now(), null));

        List<Loan> result = loanRepository.findByBook_Id(book.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void existsByUser_IdAndReturnDateIsNull_quandoTemEmprestimoAtivo_deveRetornarTrue() {
        loanRepository.save(new Loan(book, user, LocalDate.now(), null));

        Boolean result = loanRepository.existsByUser_IdAndReturnDateIsNull(user.getId());

        assertThat(result).isTrue();
    }

    @Test
    void existsByUser_IdAndReturnDateIsNull_quandoNaoTemEmprestimoAtivo_deveRetornarFalse() {
        loanRepository.save(new Loan(book, user, LocalDate.now().minusDays(3), LocalDate.now()));

        Boolean result = loanRepository.existsByUser_IdAndReturnDateIsNull(user.getId());

        assertThat(result).isFalse();
    }
}
