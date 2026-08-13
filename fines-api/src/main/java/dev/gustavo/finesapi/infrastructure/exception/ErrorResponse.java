package dev.gustavo.finesapi.infrastructure.exception;

import java.time.LocalDateTime;

public record ErrorResponse(Integer status, String message, LocalDateTime timeStamp) {
}