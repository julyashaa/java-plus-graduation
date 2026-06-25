package ru.practicum.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("Ошибка валидации типа параметра: {}", e.getMessage(), e);

        return baseError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.error("Ошибка валидации запроса: {}", e.getMessage(), e);

        var fe = e.getBindingResult().getFieldError();

        String message;
        if (fe != null) {
            message = "Field: " + fe.getField()
                    + ". Error: " + fe.getDefaultMessage()
                    + ". Value: " + fe.getRejectedValue();
        } else {
            message = "Validation failed";
        }

        return baseError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException e) {
        log.error("Нарушение ограничений валидации: {}", e.getMessage(), e);

        return baseError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.error("Отсутствует обязательный параметр запроса: {}", e.getMessage(), e);

        return baseError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(NotFoundException e) {
        log.error("Объект не найден: {}", e.getMessage(), e);

        return baseError(HttpStatus.NOT_FOUND,
                "The required object was not found.",
                e.getMessage());
    }

    @ExceptionHandler(ConditionsNotMetException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConditionsNotMet(ConditionsNotMetException e) {
        log.error("Условия выполнения операции не соблюдены: {}", e.getMessage(), e);

        return baseError(HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                e.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleBadRequestException(BadRequestException e) {
        log.error("Некорректный запрос: {}", e.getMessage(), e);

        return baseError(HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                e.getMessage());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflict(RuntimeException e) {
        log.error("Нарушение целостности данных: {}", e.getMessage(), e);

        return baseError(HttpStatus.CONFLICT,
                "Integrity constraint has been violated.",
                e.getMessage());
    }

    @ExceptionHandler(FeignException.Conflict.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleFeignConflict(FeignException.Conflict e) {
        log.error("Конфликт при вызове другого сервиса: {}", e.getMessage(), e);

        return baseError(
                HttpStatus.CONFLICT,
                "For the requested operation the conditions are not met.",
                e.contentUTF8()
        );
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleOther(Throwable e) {
        log.error("Внутренняя ошибка сервера: {}", e.getMessage(), e);

        return baseError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error.",
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