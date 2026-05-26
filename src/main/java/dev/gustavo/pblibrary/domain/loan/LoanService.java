package dev.gustavo.pblibrary.domain.loan;

import dev.gustavo.pblibrary.domain.book.Book;
import dev.gustavo.pblibrary.domain.book.BookRepository;
import dev.gustavo.pblibrary.domain.user.User;
import dev.gustavo.pblibrary.domain.user.UserRepository;
import dev.gustavo.pblibrary.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public LoanService(LoanRepository loanRepository, BookRepository bookRepository, UserRepository userRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    public LoanResponseDTO createLoan(LoanRequestDTO dto) {
        Book book = bookRepository.findById(dto.bookId()).orElseThrow(() -> new ResourceNotFoundException("Book not found."));
        User user = userRepository.findById(dto.userId()).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Loan loan = LoanMapper.toEntity(book, user);

        if (book.getAvailableCopies() <= 0) throw new BusinessException("No copies of the book available to loan.");
        if (loanRepository.findByUser_IdAndReturnDateIsNull(user.getId())) throw new BusinessException("The user already has an active loan.");

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return LoanMapper.toDTO(loanRepository.save(loan));
    }

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

        return LoanMapper.toDTO(loanRepository.save(loan));
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

    private List<LoanResponseDTO> mapToResponseList(List<Loan> loans) {
        return loans.stream().map(LoanMapper::toDTO).toList();
    }
}
