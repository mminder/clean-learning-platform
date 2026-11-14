package com.cleanlearning.application.consultation;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import java.util.Objects;

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
