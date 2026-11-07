package com.cleanlearning.domain.offering;

import java.util.Objects;
import java.util.UUID;

public record OfferingId(UUID value) {

    public OfferingId {
        Objects.requireNonNull(value, "value");
    }
}
