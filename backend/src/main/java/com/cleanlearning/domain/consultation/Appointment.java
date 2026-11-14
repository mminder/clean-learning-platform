package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * A booked appointment of a case (UC-06): one session with the case's assigned expert, from start
 * to end. It holds the appointments' rules at a given time, which the case detail and "My
 * purchases" share (glossary, spec 04 decision 21): it's {@code BOOKED} until its start and {@code
 * CONSUMED} from its start on, and upcoming until its end, so a running appointment is consumed and
 * upcoming at once; the open credits, which the booking checks, too; and UC-07's cancellation
 * rules, which the case detail's preview and the cancellation share. The column's check repeats
 * that it ends after it starts (D-36). A cancelled appointment is no {@code Appointment}: the
 * customer never sees one (spec 05, decision 3), and {@link AppointmentCancellation} holds what the
 * credits need of it.
 */
public record Appointment(
    AppointmentId id, ConsultationCaseId consultationCaseId, Instant start, Instant end) {

  /**
   * Soonest first; two appointments of a case never start at once, and the id, as text, makes the
   * order total anyway, as the order of "My purchases" does.
   */
  private static final Comparator<Appointment> BY_START =
      Comparator.comparing(Appointment::start)
          .thenComparing(appointment -> appointment.id().value().toString());

  public Appointment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException(
          "An appointment ends after it starts: " + start + " to " + end + ".");
    }
  }

  /** The upcoming appointments at {@code asOf}, the ones that haven't ended, soonest first. */
  public static List<Appointment> sortedUpcoming(List<Appointment> appointments, Instant asOf) {
    return appointments.stream()
        .filter(appointment -> appointment.isUpcomingAt(asOf))
        .sorted(BY_START)
        .toList();
  }

  /** The past appointments at {@code asOf}, the ones that have ended, newest first. */
  public static List<Appointment> sortedPast(List<Appointment> appointments, Instant asOf) {
    return appointments.stream()
        .filter(appointment -> !appointment.isUpcomingAt(asOf))
        .sorted(BY_START.reversed())
        .toList();
  }

  /**
   * Computed, never stored (D-11): the open credits of a case with this session count, this many
   * booked appointments, each of which uses a credit, consumed or not, and this many forfeited
   * credits, of appointments cancelled late (UC-07): {@code total − BOOKED − forfeited} (glossary),
   * until credit adjustments (UC-18) exist. An appointment cancelled in time is neither. The
   * booking's check, the case detail and "My purchases" all count with this one rule, so what the
   * pages show and what the booking allows can't drift apart.
   */
  public static int openCredits(
      SessionCount sessionCount, int bookedAppointments, int forfeitedCredits) {
    return sessionCount.value() - bookedAppointments - forfeitedCredits;
  }

  /**
   * Computed, never stored (D-11): whether an active case with this session count, these booked
   * appointments and this many forfeited credits is {@code COMPLETED} at {@code asOf}: no open
   * credit and no booked appointment that hasn't ended (glossary, spec 05 decision 13), so a case
   * whose last session runs isn't completed yet. The case detail, "My purchases" and the case's
   * cancellation all decide it with this one rule.
   */
  public static boolean isCompleted(
      SessionCount sessionCount,
      List<Appointment> bookedAppointments,
      int forfeitedCredits,
      Instant asOf) {
    return openCredits(sessionCount, bookedAppointments.size(), forfeitedCredits) <= 0
        && bookedAppointments.stream().noneMatch(appointment -> appointment.isUpcomingAt(asOf));
  }

  /**
   * Computed, never stored (D-11): {@code BOOKED} while the start is ahead, {@code CONSUMED} from
   * the start on.
   */
  public AppointmentStatus statusAt(Instant asOf) {
    return start.isAfter(asOf) ? AppointmentStatus.BOOKED : AppointmentStatus.CONSUMED;
  }

  /** Upcoming until it ends, also while it runs (spec 04, decision 21). */
  public boolean isUpcomingAt(Instant asOf) {
    return end.isAfter(asOf);
  }

  /**
   * Until when cancelling it is free (UC-07): {@code start − deadline}, in real hours (D-11), so 24
   * hours before a start the day after a summer-time change is another local time than the start's.
   */
  public Instant freeCancellationUntil(CancellationDeadline deadline) {
    return deadline.freeUntil(start);
  }

  /**
   * Whether its customer can cancel it at {@code asOf} (UC-07): while it's {@code BOOKED}, before
   * its start. A running appointment is consumed and can't be cancelled.
   */
  public boolean isCancellableAt(Instant asOf) {
    return statusAt(asOf) == AppointmentStatus.BOOKED;
  }

  /**
   * Whether cancelling it at {@code asOf} refunds its credit (UC-07): only before {@link
   * #freeCancellationUntil}, so at the deadline itself the cancellation is late and the credit is
   * forfeited.
   */
  public boolean isCreditRefundedIfCancelledAt(CancellationDeadline deadline, Instant asOf) {
    return asOf.isBefore(freeCancellationUntil(deadline));
  }
}
