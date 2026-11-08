package com.cleanlearning.api.offering.response;

import java.util.UUID;

import com.cleanlearning.domain.offering.ConsultationOfferingSummary;
import com.cleanlearning.domain.offering.value.OfferingStatus;

/**
 * A row of the admin's list of consultation offerings.
 */
public record ConsultationOfferingSummaryResponse(UUID id, String name, OfferingStatus status, int packageCount,
        int eligibleExpertCount) {

    public static ConsultationOfferingSummaryResponse from(ConsultationOfferingSummary summary) {
        return new ConsultationOfferingSummaryResponse(summary.id().value(), summary.name().value(), summary.status(),
                summary.packageCount(), summary.eligibleExpertCount());
    }
}
