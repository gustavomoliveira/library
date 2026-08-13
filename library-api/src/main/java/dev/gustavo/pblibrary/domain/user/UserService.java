package dev.gustavo.pblibrary.domain.user;

import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = UserMapper.toEntity(dto);
        return UserMapper.toDTO(repository.save(user));
    }

    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        user.setName(dto.name());
        user.setEmail(dto.email());
        return UserMapper.toDTO(repository.save(user));
    }

    public void deleteUser(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        repository.delete(user);
    }

    public UserResponseDTO findUserById(Long id) {
        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return UserMapper.toDTO(user);
    }

    public UserResponseDTO findUserByEmail(String email) {
        User user = repository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        return UserMapper.toDTO(user);
    }

    public List<UserResponseDTO> findUserByName(String name) {
        List<User> users = repository.findByName(name);
        return users.stream().map(UserMapper::toDTO).toList();
    }

    public List<UserResponseDTO> findAllUsers() {
        List<User> users = repository.findAll();
        return users.stream().map(UserMapper::toDTO).toList();
    }

    public List<UserResponseDTO> findUsers(String name) {
        if (name == null) return findAllUsers();
        return findUserByName(name);
    }
}
