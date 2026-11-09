package com.cleanlearning.api.consultation.request;

import java.util.UUID;

import com.cleanlearning.api.offering.request.ConsultationPackageRequest;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * The body of the purchase (UC-03 step 5): the offering, the package, and the price and session count the dialog's
 * confirmation stated. The server buys only at these (spec 03, decision 3). The price has the pattern of a package's
 * price in the offering's form. The session count is an {@code Integer}, so a missing one is a 400, not a silent 0.
 */
public record PurchaseConsultationPackageRequest(
        @NotNull UUID offeringId,
        @NotNull UUID packageId,
        @NotNull @Pattern(regexp = ConsultationPackageRequest.PRICE_PATTERN,
                message = ConsultationPackageRequest.PRICE_MESSAGE) String price,
        @NotNull @Positive Integer sessionCount) {
}
