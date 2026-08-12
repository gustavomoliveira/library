package dev.gustavo.pblibrary.domain.user;

public class UserMapper {

    public static User toEntity(UserRequestDTO dto) {
        return new User(dto.name(), dto.email());
    }

    public static UserResponseDTO toDTO(User entity) {
        return new UserResponseDTO(entity.getId(), entity.getName(), entity.getEmail());
    }
}
