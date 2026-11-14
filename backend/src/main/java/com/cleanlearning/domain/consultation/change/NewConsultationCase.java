package com.cleanlearning.domain.consultation.change;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * A new case, as a purchase creates it (UC-03 step 6): its customer, the offering, the purchase
 * terms and the purchase time. It has no assigned expert and no appointments yet.
 */
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
