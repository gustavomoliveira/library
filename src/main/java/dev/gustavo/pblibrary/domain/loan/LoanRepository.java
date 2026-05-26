package dev.gustavo.pblibrary.domain.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByReturnDateIsNull();

    List<Loan> findByUser_Id(Long userId);

    List<Loan> findByBook_Id(Long bookId);

    boolean findByUser_IdAndReturnDateIsNull(Long userId);
}
