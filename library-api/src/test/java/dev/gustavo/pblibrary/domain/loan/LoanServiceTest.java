package dev.gustavo.pblibrary.domain.loan;

import dev.gustavo.pblibrary.domain.book.Book;
import dev.gustavo.pblibrary.domain.book.BookRepository;
import dev.gustavo.pblibrary.domain.user.User;
import dev.gustavo.pblibrary.domain.user.UserRepository;
import dev.gustavo.pblibrary.exception.BusinessException;
import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanHistoryRepository loanHistoryRepository;

    @InjectMocks
    private LoanService service;

    private Book book;
    private User user;
    private Loan loan;

    @BeforeEach
    void setUp() {
        book = new Book("Clean Code", "Robert C. Martin", "978-0132350884", 5);
        book.setId(1L);

        user = new User("Gustavo Oliveira", "gustavo@email.com");
        user.setId(1L);

        loan = new Loan(book, user, LocalDate.now(), null);
        loan.setId(1L);
    }

    @Test
    void createLoan_caminhoDeSucesso_deveDecrementarCopiasESalvarLoan() {
        LoanRequestDTO dto = new LoanRequestDTO(1L, 1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(loanRepository.existsByUser_IdAndReturnDateIsNull(1L)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        LoanResponseDTO response = service.createLoan(dto);

        assertThat(response.bookId()).isEqualTo(1L);
        assertThat(book.getAvailableCopies()).isEqualTo(4);
        verify(bookRepository).save(book);
        verify(loanRepository).save(any(Loan.class));

        ArgumentCaptor<LoanHistory> historyCaptor = ArgumentCaptor.forClass(LoanHistory.class);
        verify(loanHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getEventType()).isEqualTo(LoanEventType.LOAN_CREATED);
        assertThat(historyCaptor.getValue().getLoan()).isEqualTo(loan);
    }

    @Test
    void createLoan_livroInexistente_deveLancarResourceNotFoundException() {
        LoanRequestDTO dto = new LoanRequestDTO(99L, 1L);

        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLoan(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book not found.");

        verify(loanHistoryRepository, never()).save(any());
    }

    @Test
    void createLoan_usuarioInexistente_deveLancarResourceNotFoundException() {
        LoanRequestDTO dto = new LoanRequestDTO(1L, 99L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLoan(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found.");

        verify(loanHistoryRepository, never()).save(any());
    }

    @Test
    void createLoan_semCopiasDisponiveis_deveLancarBusinessException() {
        book.setAvailableCopies(0);
        LoanRequestDTO dto = new LoanRequestDTO(1L, 1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.createLoan(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No copies of the book available to loan.");

        verify(bookRepository, never()).save(any());
        verify(loanRepository, never()).save(any());
        verify(loanHistoryRepository, never()).save(any());
    }

    @Test
    void createLoan_usuarioComEmprestimoAtivo_deveLancarBusinessException() {
        LoanRequestDTO dto = new LoanRequestDTO(1L, 1L);

        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(loanRepository.existsByUser_IdAndReturnDateIsNull(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createLoan(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("The user already has an active loan.");

        verify(bookRepository, never()).save(any());
        verify(loanHistoryRepository, never()).save(any());
    }

    @Test
    void returnLoan_caminhoDeSucesso_deveDefinirReturnDateEIncrementarCopias() {
        book.setAvailableCopies(4);
        loan.setReturnDate(null);

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        service.returnLoan(1L);

        assertThat(loan.getReturnDate()).isEqualTo(LocalDate.now());
        assertThat(book.getAvailableCopies()).isEqualTo(5);
        verify(bookRepository).save(book);

        ArgumentCaptor<LoanHistory> historyCaptor = ArgumentCaptor.forClass(LoanHistory.class);
        verify(loanHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getEventType()).isEqualTo(LoanEventType.LOAN_RETURNED);
    }

    @Test
    void returnLoan_emprestimoInexistente_deveLancarResourceNotFoundException() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.returnLoan(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(loanHistoryRepository, never()).save(any());
    }

    @Test
    void returnLoan_jaDevolvido_deveLancarBusinessException() {
        loan.setReturnDate(LocalDate.now().minusDays(1));

        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.returnLoan(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Loan already returned");

        verify(bookRepository, never()).save(any());
        verify(loanHistoryRepository, never()).save(any());
    }

    @Test
    void findAllLoans_deveRetornarTodosMapeados() {
        when(loanRepository.findAll()).thenReturn(List.of(loan));

        List<LoanResponseDTO> response = service.findAllLoans();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).bookId()).isEqualTo(1L);
    }

    @Test
    void findActiveLoans_deveRetornarApenasAtivos() {
        when(loanRepository.findByReturnDateIsNull()).thenReturn(List.of(loan));

        List<LoanResponseDTO> response = service.findActiveLoans();

        assertThat(response).hasSize(1);
    }

    @Test
    void findLoansByUser_deveRetornarEmprestimosDoUsuario() {
        when(loanRepository.findByUser_Id(1L)).thenReturn(List.of(loan));

        List<LoanResponseDTO> response = service.findLoansByUser(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).userId()).isEqualTo(1L);
    }

    @Test
    void findLoansByBook_deveRetornarEmprestimosDoLivro() {
        when(loanRepository.findByBook_Id(1L)).thenReturn(List.of(loan));

        List<LoanResponseDTO> response = service.findLoansByBook(1L);

        assertThat(response).hasSize(1);
    }

    @Test
    void findLoanHistory_quandoEmprestimoExiste_deveRetornarEventosOrdenados() {
        LoanHistory created = new LoanHistory(loan, LoanEventType.LOAN_CREATED);
        LoanHistory returned = new LoanHistory(loan, LoanEventType.LOAN_RETURNED);

        when(loanRepository.existsById(1L)).thenReturn(true);
        when(loanHistoryRepository.findByLoan_IdOrderByEventDateAsc(1L)).thenReturn(List.of(created, returned));

        List<LoanHistoryResponseDTO> response = service.findLoanHistory(1L);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).eventType()).isEqualTo(LoanEventType.LOAN_CREATED);
        assertThat(response.get(1).eventType()).isEqualTo(LoanEventType.LOAN_RETURNED);
    }

    @Test
    void findLoanHistory_quandoEmprestimoNaoExiste_deveLancarResourceNotFoundException() {
        when(loanRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findLoanHistory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Loan not found.");

        verify(loanHistoryRepository, never()).findByLoan_IdOrderByEventDateAsc(any());
    }
}
