package dev.gustavo.finesapi.infrastructure.messaging;

import dev.gustavo.finesapi.domain.fine.FineRequestDTO;
import dev.gustavo.finesapi.domain.fine.FineResponseDTO;
import dev.gustavo.finesapi.domain.fine.FineService;
import dev.gustavo.finesapi.domain.fine.FineStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanReturnedListenerTest {

    @Mock
    private FineService fineService;

    @InjectMocks
    private LoanReturnedListener listener;

    private final LoanReturnedEvent event =
            new LoanReturnedEvent(1L, 2L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 25));

    @Test
    void onLoanReturned_comAtraso_deveCriarMultaComDadosDoEvento() {
        FineResponseDTO response = new FineResponseDTO(10L, 1L, 2L, 10, BigDecimal.valueOf(30.00),
                FineStatus.PENDING, LocalDateTime.now());

        when(fineService.existsByLoanId(1L)).thenReturn(false);
        when(fineService.createFine(any(FineRequestDTO.class))).thenReturn(Optional.of(response));

        listener.onLoanReturned(event);

        ArgumentCaptor<FineRequestDTO> captor = ArgumentCaptor.forClass(FineRequestDTO.class);
        verify(fineService).createFine(captor.capture());

        FineRequestDTO request = captor.getValue();
        assertThat(request.loanId()).isEqualTo(1L);
        assertThat(request.userId()).isEqualTo(2L);
        assertThat(request.loanDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(request.returnDate()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void onLoanReturned_semAtraso_deveTerminarSemErro() {
        when(fineService.existsByLoanId(1L)).thenReturn(false);
        when(fineService.createFine(any(FineRequestDTO.class))).thenReturn(Optional.empty());

        assertThatCode(() -> listener.onLoanReturned(event)).doesNotThrowAnyException();

        verify(fineService).createFine(any(FineRequestDTO.class));
    }

    @Test
    void onLoanReturned_quandoMultaJaExiste_deveIgnorarEvento() {
        when(fineService.existsByLoanId(1L)).thenReturn(true);

        listener.onLoanReturned(event);

        verify(fineService, never()).createFine(any());
    }

    @Test
    void onLoanReturned_quandoCriacaoFalha_devePropagarExcecao() {
        when(fineService.existsByLoanId(1L)).thenReturn(false);
        when(fineService.createFine(any(FineRequestDTO.class)))
                .thenThrow(new RuntimeException("banco indisponível"));

        assertThatThrownBy(() -> listener.onLoanReturned(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("banco indisponível");
    }
}