package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.SessionCount;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The credit rules every model of a case shares, so booking, cancellation and the pages count the
 * same way.
 */
public record CaseCredits(
    SessionCount sessionCount,
    List<Appointment> bookedAppointments,
    List<CancelledAppointment> cancelledAppointments) {

  /** Ties break by id as text, which is Postgres's order of a uuid. */
  private static final Comparator<Appointment> BY_START =
      Comparator.comparing(Appointment::start)
          .thenComparing(appointment -> appointment.id().value().toString());

  public CaseCredits {
    Objects.requireNonNull(sessionCount, "sessionCount");
    bookedAppointments = List.copyOf(bookedAppointments);
    cancelledAppointments = List.copyOf(cancelledAppointments);
    Set<AppointmentId> booked =
        bookedAppointments.stream().map(Appointment::id).collect(Collectors.toSet());
    if (cancelledAppointments.stream().anyMatch(cancelled -> booked.contains(cancelled.id()))) {
      throw new IllegalArgumentException(
          "An appointment is either booked or cancelled, never both.");
    }
  }

  public void requireSessionCountOf(PurchaseTerms purchaseTerms) {
    if (!sessionCount.equals(purchaseTerms.sessionCount())) {
      throw new IllegalArgumentException(
          "A case's credits count from its purchase terms' session count.");
    }
  }

  public int totalCredits() {
    return sessionCount.value();
  }

  public int forfeitedCredits() {
    return Math.toIntExact(
        cancelledAppointments.stream().filter(CancelledAppointment::isCreditForfeited).count());
  }

  /** Every booked appointment uses a credit, consumed or not, and so does every forfeited one. */
  public int openCredits() {
    return totalCredits() - bookedAppointments.size() - forfeitedCredits();
  }

  public boolean hasOpenCredit() {
    return openCredits() > 0;
  }

  /** A case whose last session runs isn't completed yet. */
  public boolean isCompletedAt(Instant asOf) {
    return !hasOpenCredit() && nextAppointment(asOf).isEmpty();
  }

  public List<Appointment> upcomingAppointments(Instant asOf) {
    return bookedAppointments.stream()
        .filter(appointment -> appointment.isUpcomingAt(asOf))
        .sorted(BY_START)
        .toList();
  }

  public List<Appointment> pastAppointments(Instant asOf) {
    return bookedAppointments.stream()
        .filter(appointment -> !appointment.isUpcomingAt(asOf))
        .sorted(BY_START.reversed())
        .toList();
  }

  public Optional<Appointment> nextAppointment(Instant asOf) {
    return upcomingAppointments(asOf).stream().findFirst();
  }
}
