package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.NonRefundableReason;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** The one rule the case detail's preview and the cancellation share. */
public record RefundEligibility(
    Optional<NonRefundableReason> reason,
    Optional<Appointment> deadlinePassedAppointment,
    Optional<Instant> freeCancellationUntil) {

  public RefundEligibility {
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(deadlinePassedAppointment, "deadlinePassedAppointment");
    Objects.requireNonNull(freeCancellationUntil, "freeCancellationUntil");
    boolean deadlinePassed = reason.filter(NonRefundableReason.DEADLINE_PASSED::equals).isPresent();
    if (deadlinePassed != deadlinePassedAppointment.isPresent()
        || deadlinePassed != freeCancellationUntil.isPresent()) {
      throw new IllegalArgumentException(
          "The appointment and its free-cancellation time come with DEADLINE_PASSED, and only with it.");
    }
  }

  /**
   * An appointment that hasn't started is late from its free-cancellation time on, that moment
   * included: exactly when cancelling it would forfeit its credit, so a refund-eligible case
   * cancels no appointment late.
   */
  public static RefundEligibility at(
      CaseCredits credits, CancellationDeadline deadline, Instant asOf) {
    Objects.requireNonNull(credits, "credits");
    Objects.requireNonNull(deadline, "deadline");
    Objects.requireNonNull(asOf, "asOf");
    List<Appointment> bookedAppointments = credits.bookedAppointments();
    if (bookedAppointments.stream().anyMatch(a -> !a.isCancellableAt(asOf))) {
      return withReason(NonRefundableReason.SESSION_TOOK_PLACE);
    }
    if (credits.forfeitedCredits() > 0) {
      return withReason(NonRefundableReason.CREDIT_FORFEITED);
    }
    Optional<Appointment> soonestLate =
        bookedAppointments.stream()
            .filter(a -> !a.isCreditRefundedIfCancelledAt(deadline, asOf))
            .min(Comparator.comparing(Appointment::start));
    if (soonestLate.isPresent()) {
      Appointment late = soonestLate.get();
      return new RefundEligibility(
          Optional.of(NonRefundableReason.DEADLINE_PASSED),
          Optional.of(late),
          Optional.of(late.freeCancellationUntil(deadline)));
    }
    return new RefundEligibility(Optional.empty(), Optional.empty(), Optional.empty());
  }

  public boolean isRefundEligible() {
    return reason.isEmpty();
  }

  private static RefundEligibility withReason(NonRefundableReason reason) {
    return new RefundEligibility(Optional.of(reason), Optional.empty(), Optional.empty());
  }
}
