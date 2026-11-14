package com.cleanlearning.api.offering.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/**
 * The price is a string: not negative, with at most two decimals and 8 digits before the point (the
 * column is {@code numeric(10,2)}). One pattern checks all of it.
 */
public record ConsultationPackageRequest(
    UUID id,
    @NotNull @Positive Integer sessionCount,
    @NotNull @Pattern(regexp = PRICE_PATTERN, message = PRICE_MESSAGE) String price) {

  // Bean Validation matches the whole value; the anchors say so in openapi.json, where a pattern
  // matches anywhere.
  public static final String PRICE_PATTERN = "^\\d{1,8}(\\.\\d{1,2})?$";

  public static final String PRICE_MESSAGE = "must be an amount in CHF with at most two decimals";
}
