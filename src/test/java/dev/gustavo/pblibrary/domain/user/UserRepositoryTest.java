package dev.gustavo.pblibrary.domain.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Test
    void findByEmail_quandoExiste_deveRetornarOUsuario() {
        repository.save(new User("Gustavo Oliveira", "gustavo@email.com"));

        Optional<User> result = repository.findByEmail("gustavo@email.com");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Gustavo Oliveira");
    }

    @Test
    void findByEmail_quandoNaoExiste_deveRetornarVazio() {
        Optional<User> result = repository.findByEmail("naoexiste@email.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByName_deveRetornarUsuariosComEsseNome() {
        repository.save(new User("Gustavo Oliveira", "gustavo@email.com"));

        List<User> result = repository.findByName("Gustavo Oliveira");

        assertThat(result).hasSize(1);
    }
}
