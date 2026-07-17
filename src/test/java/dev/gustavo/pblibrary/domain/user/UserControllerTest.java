package dev.gustavo.pblibrary.domain.user;

import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private UserService service;

    @Test
    void createUser_deveRetornar201() throws Exception {
        UserRequestDTO dto = new UserRequestDTO("Gustavo Oliveira", "gustavo@email.com");
        UserResponseDTO response = new UserResponseDTO(1L, "Gustavo Oliveira", "gustavo@email.com");

        when(service.createUser(any(UserRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gustavo Oliveira"));
    }

    @Test
    void getUserByEmail_quandoNaoExiste_deveRetornar404() throws Exception {
        when(service.findUserByEmail("inexistente@email.com"))
                .thenThrow(new ResourceNotFoundException("User not found."));

        mockMvc.perform(get("/users/email/{email}", "inexistente@email.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
