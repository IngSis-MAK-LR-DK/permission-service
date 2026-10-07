package edu.austral.ingsis.permission.web;

import edu.austral.ingsis.permission.exception.AlreadyExistsException;
import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.exception.UnauthenticatedException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse unauthenticated(UnauthenticatedException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse forbidden(ForbiddenOperationException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse conflict(AlreadyExistsException e) {
        return new ErrorResponse(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(IllegalArgumentException e) {
        return new ErrorResponse("Valor invalido: " + e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse invalidBody(MethodArgumentNotValidException e) {
        return new ErrorResponse("El body del pedido no es valido");
    }
}
