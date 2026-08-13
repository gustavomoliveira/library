package dev.gustavo.finesapi.domain.fine;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Fine {

    private static final int LOAN_PERIOD_DAYS = 14;
    private static final BigDecimal DAILY_RATE = BigDecimal.valueOf(3.00);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long loanId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private int daysLate;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FineStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Fine(Long loanId, Long userId, int daysLate, BigDecimal amount) {
        this.loanId = loanId;
        this.userId = userId;
        this.daysLate = daysLate;
        this.amount = amount;
        this.status = FineStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public static Optional<Fine> calculate(Long loanId, Long userId, LocalDate loanDate, LocalDate returnDate) {
        LocalDate dueDate = loanDate.plusDays(LOAN_PERIOD_DAYS);
        int daysLate = (int) ChronoUnit.DAYS.between(dueDate, returnDate);

        if (daysLate <= 0) {
            return Optional.empty();
        }

        BigDecimal amount = DAILY_RATE.multiply(BigDecimal.valueOf(daysLate));
        return Optional.of(new Fine(loanId, userId, daysLate, amount));
    }

    public void pay() {
        if (this.status == FineStatus.PAID) {
            throw new FineAlreadyPaidException("Fine has already been paid.");
        }
        this.status = FineStatus.PAID;
    }
}
