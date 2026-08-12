package dev.gustavo.pblibrary.domain.loan;

import dev.gustavo.pblibrary.exception.BusinessException;
import dev.gustavo.pblibrary.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoanController.class)
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private LoanService service;

    @Test
    void createLoan_caminhoDeSucesso_deveRetornar201() throws Exception {
        LoanRequestDTO dto = new LoanRequestDTO(1L, 1L);
        LoanResponseDTO response = new LoanResponseDTO(1L, 1L, 1L, LocalDate.now(), null);

        when(service.createLoan(any(LoanRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookId").value(1L));
    }

    @Test
    void createLoan_quandoLivroNaoExiste_deveRetornar404() throws Exception {
        LoanRequestDTO dto = new LoanRequestDTO(99L, 1L);

        when(service.createLoan(any(LoanRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Book not found."));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found."));
    }

    @Test
    void createLoan_quandoRegraDeNegocioFalha_deveRetornar400() throws Exception {
        LoanRequestDTO dto = new LoanRequestDTO(1L, 1L);

        when(service.createLoan(any(LoanRequestDTO.class)))
                .thenThrow(new BusinessException("No copies of the book available to loan."));

        mockMvc.perform(post("/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnLoan_caminhoDeSucesso_deveRetornar200() throws Exception {
        LoanResponseDTO response = new LoanResponseDTO(1L, 1L, 1L, LocalDate.now().minusDays(3), LocalDate.now());

        when(service.returnLoan(anyLong())).thenReturn(response);

        mockMvc.perform(patch("/loans/{id}/return", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.returnDate").exists());
    }

    @Test
    void getActiveLoans_deveRetornar200() throws Exception {
        when(service.findActiveLoans()).thenReturn(List.of());

        mockMvc.perform(get("/loans/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getLoanHistory_quandoEmprestimoExiste_deveRetornar200() throws Exception {
        LoanHistoryResponseDTO created = new LoanHistoryResponseDTO(1L, 1L, LoanEventType.LOAN_CREATED, LocalDateTime.now().minusDays(3));
        LoanHistoryResponseDTO returned = new LoanHistoryResponseDTO(2L, 1L, LoanEventType.LOAN_RETURNED, LocalDateTime.now());

        when(service.findLoanHistory(1L)).thenReturn(List.of(created, returned));

        mockMvc.perform(get("/loans/{id}/history", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventType").value("LOAN_CREATED"))
                .andExpect(jsonPath("$[1].eventType").value("LOAN_RETURNED"));
    }

    @Test
    void getLoanHistory_quandoEmprestimoNaoExiste_deveRetornar404() throws Exception {
        when(service.findLoanHistory(99L)).thenThrow(new ResourceNotFoundException("Loan not found."));

        mockMvc.perform(get("/loans/{id}/history", 99L))
                .andExpect(status().isNotFound());
    }
}
