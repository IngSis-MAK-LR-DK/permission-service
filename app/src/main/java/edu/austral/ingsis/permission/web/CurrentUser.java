package edu.austral.ingsis.permission.web;

/** Quien hace el pedido actual. La implementacion cambia cuando se integre Auth0. */
public interface CurrentUser {

    /** Id del usuario autenticado; lanza UnauthenticatedException si no se puede determinar. */
    String userId();
}
