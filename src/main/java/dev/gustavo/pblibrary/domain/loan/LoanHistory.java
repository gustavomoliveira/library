package dev.gustavo.pblibrary.domain.loan;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanEventType eventType;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    public LoanHistory(Loan loan, LoanEventType eventType) {
        this.loan = loan;
        this.eventType = eventType;
        this.eventDate = LocalDateTime.now();
    }
}