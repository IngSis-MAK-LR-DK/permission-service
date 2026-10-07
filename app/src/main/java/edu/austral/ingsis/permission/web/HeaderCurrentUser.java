package edu.austral.ingsis.permission.web;

import edu.austral.ingsis.permission.exception.UnauthenticatedException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Implementacion PROVISORIA: toma el usuario del header X-User-Id. Se reemplaza por una que lea el
 * campo "sub" del JWT de Auth0.
 */
@Component
@RequestScope
public class HeaderCurrentUser implements CurrentUser {

    static final String HEADER = "X-User-Id";

    private final HttpServletRequest request;

    public HeaderCurrentUser(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public String userId() {
        String value = request.getHeader(HEADER);
        if (value == null || value.isBlank()) {
            throw new UnauthenticatedException("Falta el header " + HEADER);
        }
        return value;
    }
}
