package com.cleanlearning.domain.consultation.value;

import java.util.Objects;
import java.util.UUID;

public record ConsultationCaseId(UUID value) {

    public ConsultationCaseId {
        Objects.requireNonNull(value, "value");
    }
}
