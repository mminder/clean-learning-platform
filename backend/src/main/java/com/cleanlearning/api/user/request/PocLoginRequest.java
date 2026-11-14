package com.cleanlearning.api.user.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PocLoginRequest(@NotNull UUID userId) {}
