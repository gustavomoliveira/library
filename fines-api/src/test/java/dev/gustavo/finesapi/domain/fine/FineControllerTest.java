package dev.gustavo.finesapi.domain.fine;

import dev.gustavo.finesapi.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FineController.class)
class FineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private FineService service;

    @Test
    void createFine_comAtraso_deveRetornar201() throws Exception {
        FineRequestDTO dto = new FineRequestDTO(1L, 1L, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 1));
        FineResponseDTO response = new FineResponseDTO(1L, 1L, 1L, 17, BigDecimal.valueOf(51.00), FineStatus.PENDING, LocalDateTime.now());

        when(service.createFine(any(FineRequestDTO.class))).thenReturn(Optional.of(response));

        mockMvc.perform(post("/fines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.daysLate").value(17));
    }

    @Test
    void createFine_semAtraso_deveRetornar204() throws Exception {
        FineRequestDTO dto = new FineRequestDTO(2L, 1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 10));

        when(service.createFine(any(FineRequestDTO.class))).thenReturn(Optional.empty());

        mockMvc.perform(post("/fines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFineById_quandoExiste_deveRetornar200() throws Exception {
        FineResponseDTO response = new FineResponseDTO(1L, 1L, 1L, 17, BigDecimal.valueOf(51.00), FineStatus.PENDING, LocalDateTime.now());

        when(service.findFineById(1L)).thenReturn(response);

        mockMvc.perform(get("/fines/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getFineById_quandoNaoExiste_deveRetornar404() throws Exception {
        when(service.findFineById(anyLong())).thenThrow(new ResourceNotFoundException("Fine not found."));

        mockMvc.perform(get("/fines/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fine not found."));
    }

    @Test
    void getFinesByUser_deveRetornar200() throws Exception {
        when(service.findFinesByUser(1L)).thenReturn(List.of());

        mockMvc.perform(get("/fines/user/{userId}", 1L))
                .andExpect(status().isOk());
    }

    @Test
    void payFine_caminhoDeSucesso_deveRetornar200() throws Exception {
        FineResponseDTO response = new FineResponseDTO(1L, 1L, 1L, 17, BigDecimal.valueOf(51.00), FineStatus.PAID, LocalDateTime.now());

        when(service.payFine(1L)).thenReturn(response);

        mockMvc.perform(patch("/fines/{id}/pay", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void payFine_quandoJaPago_deveRetornar400() throws Exception {
        when(service.payFine(1L)).thenThrow(new FineAlreadyPaidException("Fine has already been paid."));

        mockMvc.perform(patch("/fines/{id}/pay", 1L))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payFine_quandoNaoExiste_deveRetornar404() throws Exception {
        when(service.payFine(99L)).thenThrow(new ResourceNotFoundException("Fine not found."));

        mockMvc.perform(patch("/fines/{id}/pay", 99L))
                .andExpect(status().isNotFound());
    }
}