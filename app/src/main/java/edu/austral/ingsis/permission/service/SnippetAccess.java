package edu.austral.ingsis.permission.service;

import edu.austral.ingsis.permission.model.Role;

/** Un snippet al que el usuario tiene acceso, y con que rol. */
public record SnippetAccess(String snippetId, Role role) {}
