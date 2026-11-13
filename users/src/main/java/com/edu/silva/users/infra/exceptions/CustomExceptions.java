package com.edu.silva.users.infra.exceptions;

public class CustomExceptions {
    public static class AccountNotActiveException extends RuntimeException {
        public AccountNotActiveException() {
            super("Usuário não está apto a autenticar");
        }
    }

    public static class AppAccessDeniedException extends RuntimeException {
        public AppAccessDeniedException(String app) {
            super("Usuário não possui acesso à aplicação " + app);
        }
    }

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException() {
            super("Token inválido ou expirado");
        }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("E-mail indisponível");
        }
    }

    public static class InvalidRoleException extends RuntimeException {
        public InvalidRoleException(String message) {
            super(message);
        }
    }

    public static class InvalidStatusException extends RuntimeException {
        public InvalidStatusException(String message) {
            super(message);
        }
    }

    public static class InvalidGmailTokenException extends RuntimeException {
        public InvalidGmailTokenException(String message) {
            super(message);
        }
    }

    public static class EntityNotFoundException extends RuntimeException {
        public EntityNotFoundException(String entityName, Object id) {
            super(id == null ? String.format("%s não encontrado", entityName) : String.format("%s não encontrado: %s", entityName, id));
        }
    }
}
