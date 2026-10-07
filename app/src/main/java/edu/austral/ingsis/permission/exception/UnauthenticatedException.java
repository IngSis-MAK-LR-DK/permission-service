package edu.austral.ingsis.permission.exception;

/** No se pudo determinar quien hace el pedido (HTTP 401). */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
