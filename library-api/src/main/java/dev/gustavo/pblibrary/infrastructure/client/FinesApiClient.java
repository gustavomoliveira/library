package dev.gustavo.pblibrary.infrastructure.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "fines-api")
public interface FinesApiClient {

    @PostMapping("/fines")
    ResponseEntity<Void> createFine(@RequestBody FineRequestDTO request);
}