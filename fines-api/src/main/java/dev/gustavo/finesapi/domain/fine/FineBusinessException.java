package dev.gustavo.finesapi.domain.fine;

public class FineBusinessException extends RuntimeException {
    public FineBusinessException(String message) {
        super(message);
    }
}
