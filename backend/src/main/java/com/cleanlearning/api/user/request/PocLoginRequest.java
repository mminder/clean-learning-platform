package com.cleanlearning.api.user.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** The user picked on the PoC login page. */
public record PocLoginRequest(@NotNull UUID userId) {}
