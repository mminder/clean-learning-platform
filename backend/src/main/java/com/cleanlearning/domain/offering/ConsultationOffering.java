package com.cleanlearning.domain.offering;

import java.util.Objects;
import java.util.UUID;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.value.SessionDuration;

/**
 * A consultation offering as the admin manages it (UC-16): its fields and its status. The description may be empty.
 * Packages, eligible experts and the image come with tasks 04, 05 and 07 of increment 02.
 */
public record ConsultationOffering(OfferingId id, OfferingName name, String description,
        SessionDuration sessionDuration, CancellationDeadline cancellationDeadline, OfferingStatus status) {

    public ConsultationOffering {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(sessionDuration, "sessionDuration");
        Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
        Objects.requireNonNull(status, "status");
    }

    /** A new offering starts as a draft (UC-16 step 4), with a new id. */
    public static ConsultationOffering create(OfferingName name, String description, SessionDuration sessionDuration,
            CancellationDeadline cancellationDeadline) {
        return new ConsultationOffering(new OfferingId(UUID.randomUUID()), name, description, sessionDuration,
                cancellationDeadline, OfferingStatus.DRAFT);
    }

    /** The offering with new values; id and status stay. */
    public ConsultationOffering edit(OfferingName name, String description, SessionDuration sessionDuration,
            CancellationDeadline cancellationDeadline) {
        return new ConsultationOffering(id, name, description, sessionDuration, cancellationDeadline, status);
    }
}
