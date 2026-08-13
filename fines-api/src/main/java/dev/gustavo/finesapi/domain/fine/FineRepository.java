package dev.gustavo.finesapi.domain.fine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FineRepository extends JpaRepository<Fine, Long> {

    List<Fine> findByUserId(Long userId);
}