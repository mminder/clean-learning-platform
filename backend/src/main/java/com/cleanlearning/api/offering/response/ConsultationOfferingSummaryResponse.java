package com.cleanlearning.api.offering.response;

import com.cleanlearning.domain.offering.value.OfferingStatus;
import com.cleanlearning.domain.offering.view.ConsultationOfferingSummary;
import java.util.UUID;

public record ConsultationOfferingSummaryResponse(
    UUID id, String name, OfferingStatus status, int packageCount, int eligibleExpertCount) {

  public static ConsultationOfferingSummaryResponse from(ConsultationOfferingSummary summary) {
    return new ConsultationOfferingSummaryResponse(
        summary.id().value(),
        summary.name().value(),
        summary.status(),
        summary.packageCount(),
        summary.eligibleExpertCount());
  }
}
