package edu.austral.ingsis.permission.web;

import edu.austral.ingsis.permission.exception.UnauthenticatedException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/** El usuario es el "sub" del JWT de Auth0 (por ejemplo "auth0|abc123"). */
@Component
public class JwtCurrentUser implements CurrentUser {

    @Override
    public String userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject != null && !subject.isBlank()) {
                return subject;
            }
        }
        throw new UnauthenticatedException("No hay un usuario autenticado");
    }
}
