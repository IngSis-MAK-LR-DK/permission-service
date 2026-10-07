package edu.austral.ingsis.permission.web;

import jakarta.validation.constraints.NotBlank;

public record ShareRequest(@NotBlank String userId) {}
