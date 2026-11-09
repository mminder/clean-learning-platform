package com.cleanlearning.api.user.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * The user picked on the PoC login page.
 */
public record PocLoginRequest(@NotNull UUID userId) {
}
