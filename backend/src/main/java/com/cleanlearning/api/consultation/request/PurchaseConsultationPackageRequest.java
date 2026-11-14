package com.cleanlearning.api.consultation.request;

import com.cleanlearning.api.offering.request.ConsultationPackageRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** The session count is an {@code Integer}, so a missing one is a 400, not a silent 0. */
public record PurchaseConsultationPackageRequest(
    @NotNull UUID offeringId,
    @NotNull UUID packageId,
    @NotNull
        @Pattern(
            regexp = ConsultationPackageRequest.PRICE_PATTERN,
            message = ConsultationPackageRequest.PRICE_MESSAGE)
        String price,
    @NotNull @Positive Integer sessionCount) {}
