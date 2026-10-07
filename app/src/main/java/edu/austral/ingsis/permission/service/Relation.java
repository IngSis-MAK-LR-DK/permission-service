package edu.austral.ingsis.permission.service;

import java.util.Locale;

/** Que snippets listar segun la relacion del usuario con ellos. */
public enum Relation {
    OWNER,
    SHARED,
    ALL;

    /** Acepta "owner", "shared" o "all" en cualquier mayuscula; lanza IllegalArgumentException. */
    public static Relation parse(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
