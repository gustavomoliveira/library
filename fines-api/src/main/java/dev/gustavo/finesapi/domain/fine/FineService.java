package dev.gustavo.finesapi.domain.fine;

import dev.gustavo.finesapi.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FineService {

    private final FineRepository repository;

    public FineService(FineRepository repository) {
        this.repository = repository;
    }

    public Optional<FineResponseDTO> createFine(FineRequestDTO dto) {
        Optional<Fine> fine = Fine.calculate(dto.loanId(), dto.userId(), dto.loanDate(), dto.returnDate());

        return fine.map(repository::save).map(FineMapper::toDTO);
    }

    public FineResponseDTO findFineById(Long id) {
        Fine fine = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fine not found."));
        return FineMapper.toDTO(fine);
    }

    public List<FineResponseDTO> findFinesByUser(Long userId) {
        List<Fine> fines = repository.findByUserId(userId);
        return fines.stream().map(FineMapper::toDTO).toList();
    }

    public FineResponseDTO payFine(Long id) {
        Fine fine = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Fine not found."));
        fine.pay();
        Fine savedFine = repository.save(fine);
        return FineMapper.toDTO(savedFine);
    }
}