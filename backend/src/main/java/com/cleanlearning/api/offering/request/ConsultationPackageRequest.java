package com.cleanlearning.api.offering.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * A package in the offering's form (UC-16 step 3). With an {@code id} it keeps that package of the offering; without
 * one it's a new package. The price is a string: an amount in CHF, not negative, with at most two decimals and at most
 * 8 digits before the point (the column is {@code numeric(10,2)}). One pattern checks all of it.
 */
public record ConsultationPackageRequest(
        UUID id,
        @NotNull @Positive Integer sessionCount,
        @NotNull @Pattern(regexp = PRICE_PATTERN, message = PRICE_MESSAGE) String price) {

    // Bean Validation matches the whole value; the anchors say so in openapi.json, where a pattern matches anywhere.
    public static final String PRICE_PATTERN = "^\\d{1,8}(\\.\\d{1,2})?$";

    public static final String PRICE_MESSAGE = "must be an amount in CHF with at most two decimals";
}
