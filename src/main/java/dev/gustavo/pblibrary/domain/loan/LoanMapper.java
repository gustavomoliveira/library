package dev.gustavo.pblibrary.domain.loan;

import dev.gustavo.pblibrary.domain.book.Book;
import dev.gustavo.pblibrary.domain.user.User;

import java.time.LocalDate;

public class LoanMapper {

    public static Loan toEntity(Book book, User user) {
        return new Loan(book, user, LocalDate.now(), null);
    }

    public static LoanResponseDTO toDTO(Loan entity) {
        return new LoanResponseDTO(
                entity.getId(),
                entity.getBook().getId(),
                entity.getUser().getId(),
                entity.getLoanDate(),
                entity.getReturnDate()
        );
    }
}
