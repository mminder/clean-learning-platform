package com.cleanlearning.api.offering.response;

import java.util.List;
import java.util.UUID;

import com.cleanlearning.api.response.ConsultationPackageResponse;import com.cleanlearning.domain.offering.ConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingStatus;

/**
 * A consultation offering as the admin edits it. The packages come fewest sessions first, then by id.
 */
public record ConsultationOfferingResponse(UUID id, String name, String description, OfferingStatus status,
        int sessionDurationMinutes, int cancellationDeadlineHours, List<ConsultationPackageResponse> packages) {

    public static ConsultationOfferingResponse from(ConsultationOffering offering) {
        return new ConsultationOfferingResponse(offering.id().value(), offering.name().value(),
                offering.description(), offering.status(), offering.sessionDuration().minutes(),
                offering.cancellationDeadline().hours(),
                offering.packages().stream().map(ConsultationPackageResponse::from).toList());
    }
}
