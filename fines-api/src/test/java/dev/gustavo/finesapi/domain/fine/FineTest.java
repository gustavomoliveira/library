package dev.gustavo.finesapi.domain.fine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FineTest {

    @Test
    void calculate_comAtraso_deveRetornarFineComValoresCorretos() {
        LocalDate loanDate = LocalDate.of(2026, 7, 1);
        LocalDate returnDate = LocalDate.of(2026, 8, 1);

        Optional<Fine> result = Fine.calculate(1L, 1L, loanDate, returnDate);

        assertThat(result).isPresent();
        Fine fine = result.get();
        assertThat(fine.getLoanId()).isEqualTo(1L);
        assertThat(fine.getUserId()).isEqualTo(1L);
        assertThat(fine.getDaysLate()).isEqualTo(17);
        assertThat(fine.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(51.00));
        assertThat(fine.getStatus()).isEqualTo(FineStatus.PENDING);
        assertThat(fine.getCreatedAt()).isNotNull();
    }

    @Test
    void calculate_semAtraso_deveRetornarOptionalVazio() {
        LocalDate loanDate = LocalDate.of(2026, 8, 1);
        LocalDate returnDate = LocalDate.of(2026, 8, 10);

        Optional<Fine> result = Fine.calculate(1L, 1L, loanDate, returnDate);

        assertThat(result).isEmpty();
    }

    @Test
    void calculate_devolvidoExatamenteNoPrazo_deveRetornarOptionalVazio() {
        LocalDate loanDate = LocalDate.of(2026, 8, 1);
        LocalDate returnDate = loanDate.plusDays(14);

        Optional<Fine> result = Fine.calculate(1L, 1L, loanDate, returnDate);

        assertThat(result).isEmpty();
    }

    @Test
    void pay_quandoPendente_deveAlterarStatusParaPaid() {
        Fine fine = Fine.calculate(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)).orElseThrow();

        fine.pay();

        assertThat(fine.getStatus()).isEqualTo(FineStatus.PAID);
    }

    @Test
    void pay_quandoJaPago_deveLancarFineAlreadyPaidException() {
        Fine fine = Fine.calculate(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)).orElseThrow();
        fine.pay();

        assertThatThrownBy(fine::pay)
                .isInstanceOf(FineAlreadyPaidException.class)
                .hasMessage("Fine has already been paid.");
    }
}