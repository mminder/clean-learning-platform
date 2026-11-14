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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A case as a cancellation needs it (UC-07, UC-08): its stored status, its purchase terms, which
 * hold the cancellation deadline, its booked appointments, consumed or not, and its cancelled ones.
 * It holds UC-07's and UC-08's rules; {@link #cancelAppointment} and {@link #cancel} each check
 * every one itself, in the plan's order.
 *
 * @param bookedAppointments the case's {@code BOOKED} appointments
 * @param cancelledAppointments the case's {@code CANCELLED} appointments, each with whether its
 *     credit was refunded
 */
public record CaseForCancellation(
    ConsultationCaseId id,
    CaseStatus status,
    PurchaseTerms purchaseTerms,
    List<Appointment> bookedAppointments,
    List<AppointmentCancellation> cancelledAppointments) {

  /** The appointment was cancelled before, in another tab or with its case. */
  public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";

  /** The appointment has started: it's consumed and can no longer be cancelled. */
  public static final String APPOINTMENT_STARTED = "APPOINTMENT_STARTED";

  /**
   * Whether the credit is refunded now isn't what the customer confirmed: from the dialog, the free
   * cancellation deadline passed while it was open (spec 05, decision 4).
   */
  public static final String DEADLINE_PASSED = "DEADLINE_PASSED";

  /** The case was cancelled before, in another tab; a booking on it answers this too (UC-06). */
  public static final String CASE_CANCELLED = "CASE_CANCELLED";

  /** The case is completed: no open credit, and its last appointment has ended. */
  public static final String CASE_COMPLETED = "CASE_COMPLETED";

  /**
   * Whether the case is refund-eligible now isn't what the customer confirmed: from the dialog, an
   * appointment's free cancellation deadline passed while it was open (spec 05, decision 4).
   */
  public static final String REFUND_ELIGIBILITY_CHANGED = "REFUND_ELIGIBILITY_CHANGED";

  /** {@link #CASE_CANCELLED}'s detail, for the cancellation and the booking alike. */
  static final String CASE_CANCELLED_DETAIL = "This case was cancelled.";

  public CaseForCancellation {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(purchaseTerms, "purchaseTerms");
    bookedAppointments = List.copyOf(bookedAppointments);
    cancelledAppointments = List.copyOf(cancelledAppointments);
    Set<AppointmentId> booked =
        bookedAppointments.stream().map(Appointment::id).collect(Collectors.toSet());
    if (cancelledAppointments.stream()
        .anyMatch(cancelled -> booked.contains(cancelled.appointmentId()))) {
      throw new IllegalArgumentException(
          "An appointment is either booked or cancelled, never both.");
    }
  }

  /** The credits of the appointments cancelled late, which the open credits lack (glossary). */
  public int forfeitedCredits() {
    return Math.toIntExact(
        cancelledAppointments.stream().filter(cancelled -> !cancelled.creditRefunded()).count());
  }

  /**
   * Cancels the case's appointment (UC-07 step 4), with its credit refunded if {@code now} is
   * before its free cancellation time, and only with the outcome the customer confirmed (spec 05,
   * decision 4). It checks every rule itself, in this order: the case's appointment, not cancelled
   * yet, not started, the outcome.
   *
   * @param creditRefundedConfirmed whether the dialog said the credit would be returned
   * @throws NotFoundException if the appointment is none of the case's, booked or cancelled
   * @throws BusinessRuleException {@link #APPOINTMENT_CANCELLED}, {@link #APPOINTMENT_STARTED}, or
   *     {@link #DEADLINE_PASSED} if whether the credit is refunded at {@code now} isn't what was
   *     confirmed
   */
  public AppointmentCancellation cancelAppointment(
      AppointmentId appointmentId, boolean creditRefundedConfirmed, Instant now) {
    Optional<Appointment> booked =
        bookedAppointments.stream()
            .filter(appointment -> appointment.id().equals(appointmentId))
            .findFirst();
    if (booked.isEmpty()) {
      if (cancelledAppointments.stream()
          .anyMatch(cancelled -> cancelled.appointmentId().equals(appointmentId))) {
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

  /**
   * Whether cancelling the case at {@code now} would refund its purchase price, and if not, why
   * (UC-08, D-12).
   */
  public RefundEligibility refundEligibility(Instant now) {
    return RefundEligibility.at(
        bookedAppointments, forfeitedCredits(), purchaseTerms.cancellationDeadline(), now);
  }

  /**
   * Cancels the case (UC-08 step 4), only with the outcome the customer confirmed (spec 05,
   * decision 4), and with it every booked appointment that hasn't started, each as {@link
   * #cancelAppointment} would cancel it at {@code now}: its credit refunded before its
   * free-cancellation time, forfeited after (decision 7). A running appointment is consumed and
   * stays booked. It checks every rule itself, in this order: not cancelled yet, not completed, the
   * outcome.
   *
   * @param refundEligibleConfirmed whether the dialog said the purchase price would be refunded
   * @throws BusinessRuleException {@link #CASE_CANCELLED}, {@link #CASE_COMPLETED}, or {@link
   *     #REFUND_ELIGIBILITY_CHANGED} if whether the case is refund-eligible at {@code now} isn't
   *     what was confirmed
   */
  public CaseCancellation cancel(boolean refundEligibleConfirmed, Instant now) {
    if (status == CaseStatus.CANCELLED) {
      throw new BusinessRuleException(CASE_CANCELLED, CASE_CANCELLED_DETAIL);
    }
    if (CaseCredits.isCompleted(
        purchaseTerms.sessionCount(), bookedAppointments, forfeitedCredits(), now)) {
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
        bookedAppointments.stream()
            .filter(appointment -> appointment.isCancellableAt(now))
            .map(appointment ->
                new AppointmentCancellation(
                    appointment.id(), appointment.isCreditRefundedIfCancelledAt(deadline, now)))
            .toList());
  }
}
