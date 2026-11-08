package com.cleanlearning.domain.offering.value;

import java.util.Objects;

/**
 * The name of an offering. It is never blank.
 */
public record OfferingName(String value) {

    public OfferingName {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("An offering name is not blank");
        }
    }
}
