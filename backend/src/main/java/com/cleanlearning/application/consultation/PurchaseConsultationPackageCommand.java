package com.cleanlearning.application.consultation;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import java.util.Objects;

/**
 * What the customer confirmed in the buy dialog (UC-03 steps 3–5): the offering, the package, and
 * the price and session count the confirmation stated.
 */
public record PurchaseConsultationPackageCommand(
    OfferingId offeringId,
    ConsultationPackageId packageId,
    Money price,
    SessionCount sessionCount) {

  public PurchaseConsultationPackageCommand {
    Objects.requireNonNull(offeringId, "offeringId");
    Objects.requireNonNull(packageId, "packageId");
    Objects.requireNonNull(price, "price");
    Objects.requireNonNull(sessionCount, "sessionCount");
  }
}
