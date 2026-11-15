package edu.austral.ingsis.permission.security;

import edu.austral.ingsis.permission.exception.ForbiddenOperationException;
import edu.austral.ingsis.permission.exception.UnauthenticatedException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Comprueba que el pedido lo hace un servicio de la plataforma y no un usuario con su propio token.
 *
 * <p>Los servicios se identifican con un token de Auth0 que piden con su propio client_id y secret
 * (client credentials). Ese token trae el client_id en el claim {@code azp} ("authorized party"):
 * solo se acepta el de la app M2M del handler. No hace falta repartir ninguna clave extra: la
 * identidad la sigue emitiendo Auth0, y se puede rotar o revocar desde ahi.
 *
 * <p>Sirve para las operaciones que declaran un hecho que un usuario no puede afirmar sobre si
 * mismo, como "este usuario es el dueño de este snippet nuevo".
 */
@Component
public class TrustedServiceVerifier {

    static final String AUTHORIZED_PARTY = "azp";

    private final String trustedClientId;

    public TrustedServiceVerifier(@Value("${trusted-service.client-id}") String trustedClientId) {
        if (trustedClientId == null || trustedClientId.isBlank()) {
            throw new IllegalArgumentException("trusted-service.client-id no puede estar vacio");
        }
        this.trustedClientId = trustedClientId;
    }

    /**
     * Lanza UnauthenticatedException (401) si no hay un token, y ForbiddenOperationException (403)
     * si el token no es del servicio confiable.
     */
    public void requireTrustedService() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken token)) {
            throw new UnauthenticatedException("No hay un servicio autenticado");
        }
        String clientId = token.getToken().getClaimAsString(AUTHORIZED_PARTY);
        if (!trustedClientId.equals(clientId)) {
            throw new ForbiddenOperationException(
                    "Esta operacion solo la pueden pedir los servicios de la plataforma");
        }
    }
}
