package com.edu.silva.budgets.infra.exceptions;

import com.edu.silva.common.DefaultResponse;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionsHandler {

    @ExceptionHandler(CustomExceptions.InvalidStatusException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleInvalidStatus(CustomExceptions.InvalidStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleInvalidBody(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse<Void>(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new DefaultResponse<Void>("Corpo da requisição inválido: " + ex.getMostSpecificCause().getMessage()));
    }

    @ExceptionHandler(CustomExceptions.EntityNotFoundException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleEntityNotFound(CustomExceptions.EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleGeneric(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new DefaultResponse<Void>(ex.getMessage()));
    }
}