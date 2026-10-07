package edu.austral.ingsis.permission.exception;

/** El usuario no tiene el rol necesario para la operacion (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
