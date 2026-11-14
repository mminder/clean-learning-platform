package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;
import com.cleanlearning.domain.consultation.outcome.CaseCancellation;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.CaseStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link #cancelAppointment} and {@link #cancel} each re-check every rule themselves, in the plan's
 * order.
 */
public record CaseForCancellation(
    ConsultationCaseId id, CaseStatus status, PurchaseTerms purchaseTerms, CaseCredits credits) {

  public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";

  public static final String APPOINTMENT_STARTED = "APPOINTMENT_STARTED";

  /**
   * The free-cancellation deadline passed while the dialog was open, so the refund outcome it
   * showed no longer holds.
   */
  public static final String DEADLINE_PASSED = "DEADLINE_PASSED";

  public static final String CASE_CANCELLED = "CASE_CANCELLED";

  public static final String CASE_COMPLETED = "CASE_COMPLETED";

  /**
   * An appointment's free-cancellation deadline passed while the dialog was open, so the refund
   * eligibility it showed no longer holds.
   */
  public static final String REFUND_ELIGIBILITY_CHANGED = "REFUND_ELIGIBILITY_CHANGED";

  /** Also the detail of a booking on a cancelled case. */
  static final String CASE_CANCELLED_DETAIL = "This case was cancelled.";

  public CaseForCancellation {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(purchaseTerms, "purchaseTerms");
    Objects.requireNonNull(credits, "credits");
    credits.requireSessionCountOf(purchaseTerms);
  }

  /**
   * The credit is refunded only before the free-cancellation time; rejected if that differs from
   * what the customer confirmed.
   */
  public AppointmentCancellation cancelAppointment(
      AppointmentId appointmentId, boolean creditRefundedConfirmed, Instant now) {
    Optional<Appointment> booked =
        credits.bookedAppointments().stream()
            .filter(appointment -> appointment.id().equals(appointmentId))
            .findFirst();
    if (booked.isEmpty()) {
      if (credits.cancelledAppointments().stream()
          .anyMatch(cancelled -> cancelled.id().equals(appointmentId))) {
        throw new BusinessRuleException(
            APPOINTMENT_CANCELLED, "This appointment was already cancelled.");
      }
      throw new NotFoundException("Appointment " + appointmentId.value() + " not found.");
    }
    Appointment appointment = booked.get();
    if (!appointment.isCancellableAt(now)) {
      throw new BusinessRuleException(
          APPOINTMENT_STARTED, "This appointment can no longer be cancelled.");
    }
    boolean creditRefunded =
        appointment.isCreditRefundedIfCancelledAt(purchaseTerms.cancellationDeadline(), now);
    if (creditRefunded != creditRefundedConfirmed) {
      throw new BusinessRuleException(
          DEADLINE_PASSED, "The free cancellation deadline has just passed. Please confirm again.");
    }
    return new AppointmentCancellation(appointmentId, creditRefunded);
  }

  public RefundEligibility refundEligibility(Instant now) {
    return RefundEligibility.at(credits, purchaseTerms.cancellationDeadline(), now);
  }

  /**
   * Each appointment that hasn't started is cancelled as {@link #cancelAppointment} would; a
   * running one is consumed and stays booked. Rejected if the refund eligibility differs from what
   * the customer confirmed.
   */
  public CaseCancellation cancel(boolean refundEligibleConfirmed, Instant now) {
    if (status == CaseStatus.CANCELLED) {
      throw new BusinessRuleException(CASE_CANCELLED, CASE_CANCELLED_DETAIL);
    }
    if (credits.isCompletedAt(now)) {
      throw new BusinessRuleException(CASE_COMPLETED, "This case is completed.");
    }
    boolean refundEligible = refundEligibility(now).isRefundEligible();
    if (refundEligible != refundEligibleConfirmed) {
      throw new BusinessRuleException(
          REFUND_ELIGIBILITY_CHANGED,
          "Whether the purchase price would be refunded has changed. Please confirm again.");
    }
    CancellationDeadline deadline = purchaseTerms.cancellationDeadline();
    return new CaseCancellation(
        id,
        new CancellationOutcome(now, refundEligible),
        credits.bookedAppointments().stream()
            .filter(appointment -> appointment.isCancellableAt(now))
            .map(appointment ->
                new AppointmentCancellation(
                    appointment.id(), appointment.isCreditRefundedIfCancelledAt(deadline, now)))
            .toList());
  }
}
