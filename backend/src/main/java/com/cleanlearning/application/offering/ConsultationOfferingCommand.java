package com.cleanlearning.application.offering;

import java.util.Objects;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionDuration;

/**
 * What the admin saves in the offering's form (UC-16 steps 3–4), for creating and for editing it.
 */
public record ConsultationOfferingCommand(OfferingName name, String description, SessionDuration sessionDuration,
        CancellationDeadline cancellationDeadline) {

    public ConsultationOfferingCommand {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(sessionDuration, "sessionDuration");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
    }
}
