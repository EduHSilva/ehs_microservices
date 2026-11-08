package com.edu.silva.budgets.infra.exceptions;

public class CustomExceptions {
    public static class EntityNotFoundException extends RuntimeException {
        public EntityNotFoundException(String entityName, Object id) {
            super(String.format("%s não encontrado: %s", entityName, id));
        }
    }

    public static class InvalidStatusException extends RuntimeException {
        public InvalidStatusException(String message) {
            super(message);
        }

        public InvalidStatusException(String enumName, String value) {
            super(String.format("Status inválido para %s: %s", enumName, value));
        }
    }
}
