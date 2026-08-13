package dev.gustavo.finesapi.domain.fine;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fines")
public class FineController {

    private final FineService service;

    public FineController(FineService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<FineResponseDTO> createFine(@RequestBody FineRequestDTO dto) {
        return service.createFine(dto)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FineResponseDTO> getFineById(@PathVariable Long id) {
        FineResponseDTO response = service.findFineById(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FineResponseDTO>> getFinesByUser(@PathVariable Long userId) {
        List<FineResponseDTO> response = service.findFinesByUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<FineResponseDTO> payFine(@PathVariable Long id) {
        FineResponseDTO response = service.payFine(id);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}