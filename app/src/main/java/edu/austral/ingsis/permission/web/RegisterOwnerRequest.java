package edu.austral.ingsis.permission.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Lo manda el handler con su token de servicio, asi que el dueño no sale del token: viaja aca.
 *
 * @param ownerId el {@code sub} del usuario que creo el snippet (por ejemplo {@code auth0|abc})
 */
public record RegisterOwnerRequest(@NotBlank String snippetId, @NotBlank String ownerId) {}
