package dev.gustavo.finesapi.domain.fine;

public class FineAlreadyPaidException extends FineBusinessException {
    public FineAlreadyPaidException(String message) {
        super(message);
    }
}
