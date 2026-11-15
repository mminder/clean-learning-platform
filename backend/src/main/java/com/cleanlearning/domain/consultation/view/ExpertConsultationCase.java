package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.CancelledAppointment;
import com.cleanlearning.domain.consultation.CaseCredits;
import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionDuration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A case as its assigned expert sees it (UC-11 step 4), derived at {@code asOf}, never stored
 * (D-11). Of the purchase terms it holds the session duration and the cancellation deadline, and
 * the session count in its credits, but not the price paid; of a cancellation only its time, not
 * the refund outcome (D-18). Unlike the customer, the expert sees the cancelled appointments
 * (D-12), and every note, private and public, newest first, in the order the adapter loads them
 * (UC-15). The customer's name and email are {@code String}s, as on {@code User}.
 */
public record ExpertConsultationCase(
    ConsultationCaseId id,
    String customerName,
    String customerEmail,
    OfferingName offeringName,
    SessionDuration sessionDuration,
    CancellationDeadline cancellationDeadline,
    Instant purchasedAt,
    CaseCredits credits,
    Optional<Instant> cancelledAt,
    List<Note> notes,
    Instant asOf) {

  /** Ties break by id as text, which is Postgres's order of a uuid. */
  private static final Comparator<CancelledAppointment> NEWEST_START_FIRST =
      Comparator.comparing(CancelledAppointment::start)
          .reversed()
          .thenComparing(appointment -> appointment.id().value().toString());

  public ExpertConsultationCase {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(customerName, "customerName");
    Objects.requireNonNull(customerEmail, "customerEmail");
    Objects.requireNonNull(offeringName, "offeringName");
    Objects.requireNonNull(sessionDuration, "sessionDuration");
    Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
    Objects.requireNonNull(purchasedAt, "purchasedAt");
    Objects.requireNonNull(credits, "credits");
    Objects.requireNonNull(cancelledAt, "cancelledAt");
    notes = List.copyOf(notes);
    Objects.requireNonNull(asOf, "asOf");
  }

  /** Soonest first; a running one is consumed (spec 06, decision 2). */
  public List<ExpertAppointment> upcomingAppointments() {
    return credits.upcomingAppointments(asOf).stream()
        .map(appointment -> ExpertAppointment.of(appointment, asOf))
        .toList();
  }

  /** Newest first. */
  public List<ExpertAppointment> pastAppointments() {
    return credits.pastAppointments(asOf).stream()
        .map(appointment -> ExpertAppointment.of(appointment, asOf))
        .toList();
  }

  /** Newest start first, also the ones cancelled with their case (spec 06, decision 14). */
  public List<CancelledAppointment> cancelledAppointments() {
    return credits.cancelledAppointments().stream().sorted(NEWEST_START_FIRST).toList();
  }
}
