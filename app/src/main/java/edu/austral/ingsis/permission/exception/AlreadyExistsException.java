package edu.austral.ingsis.permission.exception;

/** El permiso que se quiere crear ya existe (HTTP 409). */
public class AlreadyExistsException extends RuntimeException {

    public AlreadyExistsException(String message) {
        super(message);
    }
}
