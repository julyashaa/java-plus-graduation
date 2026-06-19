package ru.practicum.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(BadRequestException e) {
        return baseError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage());
    }

    private ApiError baseError(HttpStatus status, String reason, String message) {
        return ApiError.builder()
                .status(status.name())
                .reason(reason)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}