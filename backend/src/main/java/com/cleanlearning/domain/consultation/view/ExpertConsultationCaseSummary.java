package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.offering.value.OfferingName;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A case on "My cases" (UC-11), derived at {@code asOf}, never stored (D-11). The customer's name
 * is a {@code String}, as on {@code User}.
 */
public record ExpertConsultationCaseSummary(
    CaseInList inList, String customerName, OfferingName offeringName, Instant asOf) {

  public ExpertConsultationCaseSummary {
    Objects.requireNonNull(inList, "inList");
    Objects.requireNonNull(customerName, "customerName");
    Objects.requireNonNull(offeringName, "offeringName");
    Objects.requireNonNull(asOf, "asOf");
  }

  public Optional<ExpertAppointment> nextAppointment() {
    return inList
        .credits()
        .nextAppointment(asOf)
        .map(appointment -> ExpertAppointment.of(appointment, asOf));
  }

  public PurchaseGroup group() {
    return inList.groupAt(asOf);
  }
}
