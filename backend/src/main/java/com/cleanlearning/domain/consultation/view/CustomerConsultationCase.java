package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.Appointment;
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
 * A case as its customer sees it at {@code asOf} (UC-05's case detail): the offering's name and
 * image, read live (D-08), the purchase terms, the purchase time, the assigned expert, the
 * appointments with the upcoming ones' cancellation preview, the credits, and its cancellation, if
 * it was cancelled, or else whether it can be cancelled and whether that would refund it (UC-08).
 * What depends on the time is derived at {@code asOf}, never stored (D-11); {@link Appointment}
 * holds the appointments' rules, {@link CaseCredits} the credits and completion, and {@link
 * RefundEligibility} the refund's. A cancelled appointment isn't shown (spec 05, decision 3): only
 * its forfeited credit counts.
 *
 * @param expert the assigned expert, empty until the first booking, which assigns them; they stay
 *     after they stop being eligible (UC-06)
 * @param appointments the case's {@code BOOKED} appointments, in any order
 * @param forfeitedCredits the case's appointments cancelled late
 * @param cancellation the case's cancellation, empty while it's active
 * @param asOf the time the case is shown at, from the {@code Clock}
 */
public record CustomerConsultationCase(
    ConsultationCaseId id,
    OfferingName offeringName,
    Optional<ImageId> offeringImageId,
    PurchaseTerms purchaseTerms,
    Instant purchasedAt,
    Optional<ExpertProfile> expert,
    List<Appointment> appointments,
    int forfeitedCredits,
    Optional<CancellationOutcome> cancellation,
    Instant asOf) {

  public CustomerConsultationCase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(offeringName, "offeringName");
    Objects.requireNonNull(offeringImageId, "offeringImageId");
    Objects.requireNonNull(purchaseTerms, "purchaseTerms");
    Objects.requireNonNull(purchasedAt, "purchasedAt");
    Objects.requireNonNull(expert, "expert");
    appointments = List.copyOf(appointments);
    if (forfeitedCredits < 0) {
      throw new IllegalArgumentException(
          "A case has no negative number of forfeited credits: " + forfeitedCredits);
    }
    Objects.requireNonNull(cancellation, "cancellation");
    Objects.requireNonNull(asOf, "asOf");
  }

  /** The purchase terms' session count, until credit adjustments exist (UC-18). */
  public int totalCredits() {
    return purchaseTerms.sessionCount().value();
  }

  /**
   * Every booked appointment uses a credit, consumed or not, and so does every forfeited one,
   * counted as the booking counts them.
   */
  public int openCredits() {
    return CaseCredits.openCredits(
        purchaseTerms.sessionCount(), appointments.size(), forfeitedCredits);
  }

  /**
   * Whether the customer can book an appointment: the case is active and an open credit is left.
   */
  public boolean canBook() {
    return cancellation.isEmpty() && openCredits() > 0;
  }

  /**
   * Whether the customer can cancel the case (UC-08): it's active and not completed, so also while
   * its last session runs.
   */
  public boolean canCancel() {
    return cancellation.isEmpty()
        && !CaseCredits.isCompleted(
            purchaseTerms.sessionCount(), appointments, forfeitedCredits, asOf);
  }

  /**
   * Whether cancelling the case at {@code asOf} would refund its purchase price, and if not, why:
   * the cancel dialog's preview (spec 05, decision 12), the rule the cancellation decides with.
   * Present exactly when {@link #canCancel}.
   */
  public Optional<RefundEligibility> refundIfCancelledNow() {
    if (!canCancel()) {
      return Optional.empty();
    }
    return Optional.of(
        RefundEligibility.at(
            appointments, forfeitedCredits, purchaseTerms.cancellationDeadline(), asOf));
  }

  /**
   * The appointments that haven't ended, soonest first; a running one is {@code CONSUMED} (spec 04,
   * decision 21) and can't be cancelled. Each with its cancellation preview at {@code asOf}, from
   * the case's cancellation deadline.
   */
  public List<CustomerUpcomingAppointment> upcomingAppointments() {
    return Appointment.sortedUpcoming(appointments, asOf).stream()
        .map(appointment ->
            CustomerUpcomingAppointment.of(appointment, purchaseTerms.cancellationDeadline(), asOf))
        .toList();
  }

  /** The appointments that have ended, newest first, all {@code CONSUMED}. */
  public List<CustomerAppointment> pastAppointments() {
    return CustomerAppointment.past(appointments, asOf);
  }
}
