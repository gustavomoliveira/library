package dev.gustavo.pblibrary.domain.loan;

import dev.gustavo.pblibrary.domain.book.Book;
import dev.gustavo.pblibrary.domain.book.BookRepository;
import dev.gustavo.pblibrary.domain.user.User;
import dev.gustavo.pblibrary.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// Nota: se o seu Spring Boot for anterior à 3.4, troque @MockitoBean por
// @MockBean (import org.springframework.boot.test.mock.mockito.MockBean).

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Este teste NÃO usa mocks para BookRepository/UserRepository — eles são os beans reais,
 * conectados ao banco H2 de teste (ver src/test/resources/application.properties).
 * Apenas o LoanRepository é substituído por um mock, para forçar uma falha depois que
 * o Book já foi salvo — exatamente o cenário que o @Transactional precisa cobrir.
 */
@SpringBootTest
class LoanTransactionIntegrationTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private LoanRepository loanRepository;

    @Test
    void createLoan_deveDesfazerDecrementoDeCopias_quandoSalvarOEmprestimoFalhar() {
        Book book = bookRepository.save(new Book("Clean Code", "Robert C. Martin", "978-0132350884", 1));
        User user = userRepository.save(new User("Gustavo Oliveira", "gustavo@email.com"));

        when(loanRepository.existsByUser_IdAndReturnDateIsNull(user.getId())).thenReturn(false);
        when(loanRepository.save(any(Loan.class)))
                .thenThrow(new RuntimeException("Falha simulada ao salvar o empréstimo"));

        LoanRequestDTO dto = new LoanRequestDTO(book.getId(), user.getId());

        assertThrows(RuntimeException.class, () -> loanService.createLoan(dto));

        Book bookAposFalha = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(bookAposFalha.getAvailableCopies())
                .as("O availableCopies deveria ter voltado ao valor original, já que a transação inteira deve ser desfeita")
                .isEqualTo(1);
    }
}
