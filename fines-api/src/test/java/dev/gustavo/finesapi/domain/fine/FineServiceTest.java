package dev.gustavo.finesapi.domain.fine;

import dev.gustavo.finesapi.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FineServiceTest {

    @Mock
    private FineRepository repository;

    @InjectMocks
    private FineService service;

    private Fine fine;

    @BeforeEach
    void setUp() {
        fine = Fine.calculate(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)).orElseThrow();
    }

    @Test
    void createFine_comAtraso_deveSalvarERetornarDTOPreenchido() {
        FineRequestDTO dto = new FineRequestDTO(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));

        when(repository.save(any(Fine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<FineResponseDTO> response = service.createFine(dto);

        assertThat(response).isPresent();
        assertThat(response.get().daysLate()).isEqualTo(17);
        verify(repository).save(any(Fine.class));
    }

    @Test
    void createFine_semAtraso_naoDeveSalvarERetornarOptionalVazio() {
        FineRequestDTO dto = new FineRequestDTO(2L, 1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10));

        Optional<FineResponseDTO> response = service.createFine(dto);

        assertThat(response).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void findFineById_quandoExiste_deveRetornarDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(fine));

        FineResponseDTO response = service.findFineById(1L);

        assertThat(response.loanId()).isEqualTo(1L);
    }

    @Test
    void findFineById_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findFineById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findFinesByUser_deveRetornarListaDeDTOs() {
        when(repository.findByUserId(1L)).thenReturn(List.of(fine));

        List<FineResponseDTO> response = service.findFinesByUser(1L);

        assertThat(response).hasSize(1);
    }

    @Test
    void payFine_quandoExiste_deveAlterarStatusESalvar() {
        when(repository.findById(1L)).thenReturn(Optional.of(fine));
        when(repository.save(any(Fine.class))).thenReturn(fine);

        FineResponseDTO response = service.payFine(1L);

        assertThat(response.status()).isEqualTo(FineStatus.PAID);
        verify(repository).save(fine);
    }

    @Test
    void payFine_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.payFine(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void payFine_quandoJaPago_devePropagarFineAlreadyPaidException() {
        fine.pay();
        when(repository.findById(1L)).thenReturn(Optional.of(fine));

        assertThatThrownBy(() -> service.payFine(1L))
                .isInstanceOf(FineAlreadyPaidException.class);
    }
}