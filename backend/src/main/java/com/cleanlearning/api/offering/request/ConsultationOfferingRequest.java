package com.cleanlearning.api.offering.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * The body of {@code POST} and {@code PUT}: the offering's form (UC-16 steps 3–4). The annotations repeat UC-16's
 * rules, so a 400 names every invalid field; the value objects stay the authority. The numbers are {@code Integer}, so
 * a missing one is a 400, not a silent 0.
 */
public record ConsultationOfferingRequest(
        @NotBlank String name,
        @NotNull String description,
        @NotNull @Positive Integer sessionDurationMinutes,
        @NotNull @PositiveOrZero Integer cancellationDeadlineHours) {
}
