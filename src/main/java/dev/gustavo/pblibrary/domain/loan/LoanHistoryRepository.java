package dev.gustavo.pblibrary.domain.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanHistoryRepository extends JpaRepository<LoanHistory, Long> {

    List<LoanHistory> findByLoan_IdOrderByEventDateAsc(Long loanId);
}