package com.edu.silva.tasks.infra.exceptions;

public class CustomExceptions {
    public static class EntityNotFoundException extends RuntimeException {
        public EntityNotFoundException(String entityName, Object id) {
            super(String.format("%s não encontrado: %s", entityName, id));
        }
    }

    public static class InvalidTaskCompletionException extends RuntimeException {
        public InvalidTaskCompletionException(String message) {
            super(message);
        }
    }
}
