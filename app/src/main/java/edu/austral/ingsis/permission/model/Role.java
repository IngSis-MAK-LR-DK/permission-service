package edu.austral.ingsis.permission.model;

/** Rol de un usuario sobre un snippet. NONE no se guarda: significa "sin acceso". */
public enum Role {
    OWNER,
    READER,
    NONE
}
