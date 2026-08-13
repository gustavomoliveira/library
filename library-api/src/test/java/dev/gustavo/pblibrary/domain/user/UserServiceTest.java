package dev.gustavo.pblibrary.domain.user;

import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Gustavo Oliveira", "gustavo@email.com");
        user.setId(1L);
    }

    @Test
    void createUser_deveSalvarERetornarDTO() {
        when(repository.save(any(User.class))).thenReturn(user);

        UserRequestDTO dto = new UserRequestDTO("Gustavo Oliveira", "gustavo@email.com");
        UserResponseDTO response = service.createUser(dto);

        assertThat(response.name()).isEqualTo("Gustavo Oliveira");
        verify(repository).save(any(User.class));
    }

    @Test
    void updateUser_quandoExiste_deveAtualizarERetornarDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenReturn(user);

        UserRequestDTO dto = new UserRequestDTO("Gustavo O.", "gustavo@email.com");
        UserResponseDTO response = service.updateUser(1L, dto);

        assertThat(response.name()).isEqualTo("Gustavo O.");
    }

    @Test
    void updateUser_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        UserRequestDTO dto = new UserRequestDTO("Nome", "email@email.com");

        assertThatThrownBy(() -> service.updateUser(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_quandoExiste_deveDeletar() {
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        service.deleteUser(1L);

        verify(repository).delete(user);
    }

    @Test
    void deleteUser_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findUserById_quandoExiste_deveRetornarDTO() {
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDTO response = service.findUserById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void findUserById_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findUserByEmail_quandoExiste_deveRetornarDTO() {
        when(repository.findByEmail("gustavo@email.com")).thenReturn(Optional.of(user));

        UserResponseDTO response = service.findUserByEmail("gustavo@email.com");

        assertThat(response.email()).isEqualTo("gustavo@email.com");
    }

    @Test
    void findUserByEmail_quandoNaoExiste_deveLancarResourceNotFoundException() {
        when(repository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findUserByEmail("inexistente@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findUserByName_deveRetornarListaDeDTOs() {
        when(repository.findByName("Gustavo Oliveira")).thenReturn(List.of(user));

        List<UserResponseDTO> response = service.findUserByName("Gustavo Oliveira");

        assertThat(response).hasSize(1);
    }

    @Test
    void findAllUsers_deveRetornarTodosOsUsuarios() {
        when(repository.findAll()).thenReturn(List.of(user));

        List<UserResponseDTO> response = service.findAllUsers();

        assertThat(response).hasSize(1);
    }

    @Test
    void findUsers_semNome_deveChamarFindAllUsers() {
        when(repository.findAll()).thenReturn(List.of(user));

        service.findUsers(null);

        verify(repository).findAll();
        verify(repository, never()).findByName(any());
    }

    @Test
    void findUsers_comNome_deveChamarFindUserByName() {
        when(repository.findByName("Gustavo Oliveira")).thenReturn(List.of(user));

        service.findUsers("Gustavo Oliveira");

        verify(repository).findByName("Gustavo Oliveira");
    }
}
