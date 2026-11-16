package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;
import com.cleanlearning.domain.consultation.outcome.Booking;
import com.cleanlearning.domain.consultation.outcome.CaseCancellation;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.BookableSlot;
import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.CaseStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.ExpertProfile;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The write model of a case: what a booking and both cancellations lock and decide with (D-40).
 * {@link #book}, {@link #cancelAppointment} and {@link #cancel} each re-check every rule
 * themselves, in the plan's order; {@link #book} the status first. The eligible experts are the
 * offering's, so they are passed in.
 */
public record ConsultationCase(
    ConsultationCaseId id,
    OfferingId offeringId,
    CaseStatus status,
    PurchaseTerms purchaseTerms,
    Optional<UserId> assignedExpertId,
    CaseCredits credits) {

  public static final String NO_OPEN_CREDITS = "NO_OPEN_CREDITS";

  public static final String EXPERT_CHANGED = "EXPERT_CHANGED";

  public static final String EXPERT_NOT_ELIGIBLE = "EXPERT_NOT_ELIGIBLE";

  public static final String SLOT_NOT_AVAILABLE = "SLOT_NOT_AVAILABLE";

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

  /** The detail of a booking on a cancelled case and of a second cancellation. */
  private static final String CASE_CANCELLED_DETAIL = "This case was cancelled.";

  public ConsultationCase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(offeringId, "offeringId");
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(purchaseTerms, "purchaseTerms");
    Objects.requireNonNull(assignedExpertId, "assignedExpertId");
    Objects.requireNonNull(credits, "credits");
    credits.requireSessionCountOf(purchaseTerms);
  }

  public void requireActive() {
    if (status == CaseStatus.CANCELLED) {
      throw new BusinessRuleException(CASE_CANCELLED, CASE_CANCELLED_DETAIL);
    }
  }

  public void requireOpenCredit() {
    if (!credits.hasOpenCredit()) {
      throw new BusinessRuleException(NO_OPEN_CREDITS, "This case has no open credits.");
    }
  }

  /**
   * The case's expert stays after they stop being eligible; only a first booking needs an eligible
   * one.
   */
  public UserId expertFor(UserId named, List<ExpertProfile> eligibleExperts) {
    if (assignedExpertId.isPresent()) {
      if (!assignedExpertId.get().equals(named)) {
        throw new BusinessRuleException(
            EXPERT_CHANGED, "This case already has an expert. Please choose a time with them.");
      }
      return named;
    }
    if (eligibleExperts.stream().noneMatch(expert -> expert.id().equals(named))) {
      throw new BusinessRuleException(
          EXPERT_NOT_ELIGIBLE,
          "This expert is no longer available for this offering. Please choose another.");
    }
    return named;
  }

  /**
   * A slot has no free-cancellation time when the case's deadline is longer than the minimum
   * notice: that time has passed already (D-11).
   */
  public List<BookableSlot> bookableSlots(SlotFinder slotFinder, Instant now) {
    return slotFinder.startTimes(purchaseTerms.sessionDuration(), now).stream()
        .map(start -> new BookableSlot(start, endOf(start), freeCancellationUntil(start, now)))
        .toList();
  }

  private Optional<Instant> freeCancellationUntil(Instant start, Instant now) {
    return Optional.of(purchaseTerms.cancellationDeadline().freeUntil(start))
        .filter(until -> until.isAfter(now));
  }

  /** Checks every rule in this order: active case, open credit, expert, slot. */
  public Booking book(
      UserId expertId,
      Instant start,
      List<ExpertProfile> eligibleExperts,
      SlotFinder slotFinder,
      Instant now) {
    requireActive();
    requireOpenCredit();
    UserId expert = expertFor(expertId, eligibleExperts);
    if (!slotFinder.isBookable(start, purchaseTerms.sessionDuration(), now)) {
      throw new BusinessRuleException(SLOT_NOT_AVAILABLE, "This time is no longer available.");
    }
    Appointment appointment =
        new Appointment(new AppointmentId(UUID.randomUUID()), id, start, endOf(start));
    return new Booking(appointment, expert, assignedExpertId.isEmpty());
  }

  private Instant endOf(Instant start) {
    return start.plus(Duration.ofMinutes(purchaseTerms.sessionDuration().minutes()));
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
    requireActive();
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
