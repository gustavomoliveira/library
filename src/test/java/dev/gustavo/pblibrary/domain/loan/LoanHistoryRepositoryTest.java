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
class LoanHistoryRepositoryTest {

    @Autowired
    private LoanHistoryRepository loanHistoryRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    private Loan loan;

    @BeforeEach
    void setUp() {
        Book book = bookRepository.save(new Book("Clean Code", "Robert C. Martin", "978-0132350884", 5));
        User user = userRepository.save(new User("Gustavo Oliveira", "gustavo@email.com"));
        loan = loanRepository.save(new Loan(book, user, LocalDate.now(), null));
    }

    @Test
    void findByLoan_IdOrderByEventDateAsc_deveRetornarEventosNaOrdemCorreta() throws InterruptedException {
        // O evento CREATED é construído primeiro (eventDate mais antigo), mas
        // salvo por último — de propósito, para provar que a query ordena pelo
        // eventDate real, e não pela ordem em que os registros foram inseridos.
        LoanHistory created = new LoanHistory(loan, LoanEventType.LOAN_CREATED);
        Thread.sleep(5);
        LoanHistory returned = new LoanHistory(loan, LoanEventType.LOAN_RETURNED);

        loanHistoryRepository.save(returned);
        loanHistoryRepository.save(created);

        List<LoanHistory> result = loanHistoryRepository.findByLoan_IdOrderByEventDateAsc(loan.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEventType()).isEqualTo(LoanEventType.LOAN_CREATED);
        assertThat(result.get(1).getEventType()).isEqualTo(LoanEventType.LOAN_RETURNED);
    }

    @Test
    void findByLoan_IdOrderByEventDateAsc_quandoNaoHaHistorico_deveRetornarListaVazia() {
        List<LoanHistory> result = loanHistoryRepository.findByLoan_IdOrderByEventDateAsc(loan.getId());

        assertThat(result).isEmpty();
    }
}
