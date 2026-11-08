package com.cleanlearning.domain.user.value;

import java.util.Objects;

/**
 * The name an expert shows publicly, from their expert profile. It is never blank.
 */
public record DisplayName(String value) {

    public DisplayName {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A display name is not blank");
        }
    }
}
