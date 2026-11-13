package com.edu.silva.users.infra.exceptions;

import com.edu.silva.common.DefaultResponse;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionsHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionsHandler.class);

    @ExceptionHandler(CustomExceptions.EntityNotFoundException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleEntityNotFound(CustomExceptions.EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.InvalidTokenException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleInvalidToken(CustomExceptions.InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.AccountNotActiveException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleAccountNotActive(CustomExceptions.AccountNotActiveException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new DefaultResponse<Void>(ex.getMessage()));
    }

    @ExceptionHandler(CustomExceptions.AppAccessDeniedException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleAppAccessDenied(CustomExceptions.AppAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new DefaultResponse<Void>(ex.getMessage()));
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

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new DefaultResponse<Void>("Acesso negado"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<@NonNull DefaultResponse<Void>> handleGeneric(RuntimeException ex) {
        log.error("Erro inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new DefaultResponse<Void>(ex.getMessage()));
    }
}