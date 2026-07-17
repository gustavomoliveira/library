package dev.gustavo.pblibrary.domain.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Nota: se o seu Spring Boot for anterior à 3.4, troque @MockitoBean por
// @MockBean (import org.springframework.boot.test.mock.mockito.MockBean).
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService service;

    @Test
    void createBook_deveRetornar201() throws Exception {
        BookRequestDTO dto = new BookRequestDTO("Clean Code", "Robert C. Martin", "978-0132350884", 5);
        BookResponseDTO response = new BookResponseDTO(1L, "Clean Code", "Robert C. Martin", "978-0132350884", 5, 5);

        when(service.createBook(any(BookRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Clean Code"));
    }

    @Test
    void getBookById_quandoNaoExiste_deveRetornar404() throws Exception {
        when(service.findBookById(anyLong())).thenThrow(new ResourceNotFoundException("Book not found."));

        mockMvc.perform(get("/books/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found."));
    }

    @Test
    void deleteBook_deveRetornar204() throws Exception {
        mockMvc.perform(delete("/books/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    void getBooks_semParametros_deveRetornar200() throws Exception {
        when(service.findBooks(null, null)).thenReturn(List.of());

        mockMvc.perform(get("/books"))
                .andExpect(status().isOk());
    }
}
