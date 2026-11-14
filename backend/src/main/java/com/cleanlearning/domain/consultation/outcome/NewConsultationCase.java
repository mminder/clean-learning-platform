package com.cleanlearning.domain.consultation.outcome;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.Objects;

public record NewConsultationCase(
    ConsultationCaseId id,
    UserId customerId,
    OfferingId offeringId,
    PurchaseTerms purchaseTerms,
    Instant purchasedAt) {

  public NewConsultationCase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(customerId, "customerId");
    Objects.requireNonNull(offeringId, "offeringId");
    Objects.requireNonNull(purchaseTerms, "purchaseTerms");
    Objects.requireNonNull(purchasedAt, "purchasedAt");
  }
}
