package com.edu.silva.users.infra.exceptions;

import com.edu.silva.common.DefaultResponse;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionsHandler {

    @ExceptionHandler(CustomExceptions.EntityNotFoundException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleEntityNotFound(CustomExceptions.EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.InvalidRoleException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleInvalidRole(CustomExceptions.InvalidRoleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.InvalidStatusException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleInvalidStatus(CustomExceptions.InvalidStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.EmailAlreadyExistsException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleInvalidEmail(CustomExceptions.EmailAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.InvalidGmailTokenException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleInvalidGmailToken(CustomExceptions.InvalidGmailTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleBadCredential(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleGeneric(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new DefaultResponse<Void>(ex.getMessage()));
    }
}