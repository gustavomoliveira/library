package dev.gustavo.finesapi.domain.fine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class FineRepositoryTest {

    @Autowired
    private FineRepository repository;

    @Test
    void findByUserId_deveRetornarMultasDoUsuario() {
        Fine fine = Fine.calculate(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1)).orElseThrow();
        repository.save(fine);

        List<Fine> result = repository.findByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoanId()).isEqualTo(1L);
    }

    @Test
    void findByUserId_quandoNaoHaMultas_deveRetornarListaVazia() {
        List<Fine> result = repository.findByUserId(99L);

        assertThat(result).isEmpty();
    }
}