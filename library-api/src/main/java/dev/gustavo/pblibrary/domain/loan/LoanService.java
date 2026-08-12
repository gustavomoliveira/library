package dev.gustavo.pblibrary.domain.loan;

import dev.gustavo.pblibrary.domain.book.Book;
import dev.gustavo.pblibrary.domain.book.BookRepository;
import dev.gustavo.pblibrary.domain.user.User;
import dev.gustavo.pblibrary.domain.user.UserRepository;
import dev.gustavo.pblibrary.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final LoanHistoryRepository loanHistoryRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository,
                       UserRepository userRepository, LoanHistoryRepository loanHistoryRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.loanHistoryRepository = loanHistoryRepository;
    }

    @Transactional
    public LoanResponseDTO createLoan(LoanRequestDTO dto) {
        Book book = bookRepository.findById(dto.bookId()).orElseThrow(() -> new ResourceNotFoundException("Book not found."));
        User user = userRepository.findById(dto.userId()).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Loan loan = LoanMapper.toEntity(book, user);

        if (book.getAvailableCopies() <= 0) throw new BusinessException("No copies of the book available to loan.");
        if (loanRepository.existsByUser_IdAndReturnDateIsNull(user.getId())) throw new BusinessException("The user already has an active loan.");

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Loan savedLoan = loanRepository.save(loan);
        loanHistoryRepository.save(new LoanHistory(savedLoan, LoanEventType.LOAN_CREATED));

        return LoanMapper.toDTO(savedLoan);
    }

    @Transactional
    public LoanResponseDTO returnLoan(Long id) {
        Loan loan = loanRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Loan not found."));

        if (loan.getReturnDate() == null) {
            loan.setReturnDate(LocalDate.now());
        } else {
            throw new BusinessException("Loan already returned");
        }

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        Loan savedLoan = loanRepository.save(loan);
        loanHistoryRepository.save(new LoanHistory(savedLoan, LoanEventType.LOAN_RETURNED));

        return LoanMapper.toDTO(savedLoan);
    }

    public List<LoanResponseDTO> findAllLoans() {
        List<Loan> loans = loanRepository.findAll();
        return mapToResponseList(loans);
    }

    public List<LoanResponseDTO> findActiveLoans() {
        List<Loan> loans = loanRepository.findByReturnDateIsNull();
        return mapToResponseList(loans);
    }

    public List<LoanResponseDTO> findLoansByUser(Long id) {
        List<Loan> loans = loanRepository.findByUser_Id(id);
        return mapToResponseList(loans);
    }

    public List<LoanResponseDTO> findLoansByBook(Long id) {
        List<Loan> loans = loanRepository.findByBook_Id(id);
        return mapToResponseList(loans);
    }

    public List<LoanHistoryResponseDTO> findLoanHistory(Long loanId) {
        if (!loanRepository.existsById(loanId)) throw new ResourceNotFoundException("Loan not found.");
        List<LoanHistory> history = loanHistoryRepository.findByLoan_IdOrderByEventDateAsc(loanId);
        return history.stream().map(LoanHistoryMapper::toDTO).toList();
    }

    private List<LoanResponseDTO> mapToResponseList(List<Loan> loans) {
        return loans.stream().map(LoanMapper::toDTO).toList();
    }
}