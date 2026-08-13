package dev.gustavo.pblibrary.domain.book;

import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private BookService service;

    @Test
    void createBook_deveRetornar201() throws Exception {
        BookRequestDTO dto = new BookRequestDTO("Clean Code", "Robert C. Martin", "978-0132350884", 5);
        BookResponseDTO response = new BookResponseDTO(1L, "Clean Code", "Robert C. Martin", "978-0132350884", 5, 5);

        when(service.createBook(any(BookRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
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
