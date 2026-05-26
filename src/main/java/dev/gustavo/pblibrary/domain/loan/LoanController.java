package dev.gustavo.pblibrary.domain.loan;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LoanResponseDTO> createLoan(@RequestBody LoanRequestDTO dto) {
        LoanResponseDTO response = service.createLoan(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/return")
    public ResponseEntity<LoanResponseDTO> returnLoan(@PathVariable Long id) {
        LoanResponseDTO response = service.returnLoan(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoanResponseDTO>> getAllLoans() {
        List<LoanResponseDTO> response = service.findAllLoans();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<LoanResponseDTO>> getActiveLoans() {
        List<LoanResponseDTO> response = service.findActiveLoans();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<LoanResponseDTO>> getUserLoans(@PathVariable Long id) {
        List<LoanResponseDTO> response = service.findLoansByUser(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/book/{id}")
    public ResponseEntity<List<LoanResponseDTO>> getBookLoans(@PathVariable Long id) {
        List<LoanResponseDTO> response = service.findLoansByBook(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
