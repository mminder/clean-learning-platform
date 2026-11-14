package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.CaseCredits;
import com.cleanlearning.domain.consultation.RefundEligibility;
import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.user.ExpertProfile;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * What depends on the time is derived at {@code asOf}, never stored (D-11). A cancelled appointment
 * isn't shown: only its forfeited credit counts.
 */
public record CustomerConsultationCase(
    ConsultationCaseId id,
    OfferingName offeringName,
    Optional<ImageId> offeringImageId,
    PurchaseTerms purchaseTerms,
    Instant purchasedAt,
    Optional<ExpertProfile> expert,
    CaseCredits credits,
    Optional<CancellationOutcome> cancellation,
    Instant asOf) {

  public CustomerConsultationCase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(offeringName, "offeringName");
    Objects.requireNonNull(offeringImageId, "offeringImageId");
    Objects.requireNonNull(purchaseTerms, "purchaseTerms");
    Objects.requireNonNull(purchasedAt, "purchasedAt");
    Objects.requireNonNull(expert, "expert");
    Objects.requireNonNull(credits, "credits");
    credits.requireSessionCountOf(purchaseTerms);
    Objects.requireNonNull(cancellation, "cancellation");
    Objects.requireNonNull(asOf, "asOf");
  }

  public boolean canBook() {
    return cancellation.isEmpty() && credits.hasOpenCredit();
  }

  /** Active and not completed, so also while the last session runs. */
  public boolean canCancel() {
    return cancellation.isEmpty() && !credits.isCompletedAt(asOf);
  }

  public Optional<RefundEligibility> refundIfCancelledNow() {
    if (!canCancel()) {
      return Optional.empty();
    }
    return Optional.of(RefundEligibility.at(credits, purchaseTerms.cancellationDeadline(), asOf));
  }

  public List<CustomerUpcomingAppointment> upcomingAppointments() {
    return credits.upcomingAppointments(asOf).stream()
        .map(appointment ->
            CustomerUpcomingAppointment.of(appointment, purchaseTerms.cancellationDeadline(), asOf))
        .toList();
  }

  public List<CustomerAppointment> pastAppointments() {
    return credits.pastAppointments(asOf).stream()
        .map(appointment -> CustomerAppointment.of(appointment, asOf))
        .toList();
  }
}
