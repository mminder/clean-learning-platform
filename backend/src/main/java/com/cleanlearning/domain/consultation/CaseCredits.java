package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.offering.value.SessionCount;
import java.time.Instant;
import java.util.List;

/**
 * The case-level rules on a case's credits, which the booking, the cancellation and the views
 * share.
 */
public final class CaseCredits {

  private CaseCredits() {}

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
}
