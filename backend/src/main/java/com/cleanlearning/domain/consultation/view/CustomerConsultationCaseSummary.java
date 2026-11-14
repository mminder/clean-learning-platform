package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.user.value.DisplayName;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Derived at {@code asOf}, never stored (D-11). */
public record CustomerConsultationCaseSummary(
    CaseInList inList,
    OfferingName offeringName,
    Optional<ImageId> offeringImageId,
    Optional<DisplayName> expertDisplayName,
    Instant asOf) {

  public CustomerConsultationCaseSummary {
    Objects.requireNonNull(inList, "inList");
    Objects.requireNonNull(offeringName, "offeringName");
    Objects.requireNonNull(offeringImageId, "offeringImageId");
    Objects.requireNonNull(expertDisplayName, "expertDisplayName");
    Objects.requireNonNull(asOf, "asOf");
  }

  public Optional<CustomerAppointment> nextAppointment() {
    return inList
        .credits()
        .nextAppointment(asOf)
        .map(appointment -> CustomerAppointment.of(appointment, asOf));
  }

  public PurchaseGroup group() {
    return inList.groupAt(asOf);
  }
}
