package com.cleanlearning.api.offering.response;

import java.util.UUID;

import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingStatus;

/**
 * A consultation offering as the admin edits it.
 */
public record ConsultationOfferingResponse(UUID id, String name, String description, OfferingStatus status,
        int sessionDurationMinutes, int cancellationDeadlineHours) {

    public static ConsultationOfferingResponse from(ConsultationOffering offering) {
        return new ConsultationOfferingResponse(offering.id().value(), offering.name().value(),
                offering.description(), offering.status(), offering.sessionDuration().minutes(),
                offering.cancellationDeadline().hours());
    }
}
