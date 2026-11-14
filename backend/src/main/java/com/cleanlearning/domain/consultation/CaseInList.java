package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/** What every "My …" list needs to group and sort its cases with one rule. */
public record CaseInList(
    ConsultationCaseId id,
    Instant purchasedAt,
    Optional<Instant> cancelledAt,
    CaseCredits credits) {

  public CaseInList {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(purchasedAt, "purchasedAt");
    Objects.requireNonNull(cancelledAt, "cancelledAt");
    Objects.requireNonNull(credits, "credits");
  }

  /**
   * Ties break by id as text, which is Postgres's order of a uuid; {@code UUID.compareTo} (signed)
   * isn't.
   */
  public static Comparator<CaseInList> orderAt(Instant asOf) {
    return Comparator.comparing((CaseInList listed) -> listed.groupAt(asOf))
        .thenComparing((first, second) -> compareWithinGroup(first, second, asOf))
        .thenComparing(listed -> listed.id().value().toString());
  }

  public PurchaseGroup groupAt(Instant asOf) {
    if (cancelledAt.isPresent()) {
      return PurchaseGroup.PAST;
    }
    if (credits.nextAppointment(asOf).isPresent()) {
      return PurchaseGroup.UPCOMING;
    }
    return credits.isCompletedAt(asOf) ? PurchaseGroup.PAST : PurchaseGroup.NEEDS_BOOKING;
  }

  private static int compareWithinGroup(CaseInList first, CaseInList second, Instant asOf) {
    return switch (first.groupAt(asOf)) {
      case UPCOMING -> first.nextStart(asOf).compareTo(second.nextStart(asOf));
      case NEEDS_BOOKING -> first.purchasedAt.compareTo(second.purchasedAt);
      case PAST -> second.pastOrderKey(asOf).compareTo(first.pastOrderKey(asOf));
    };
  }

  private Instant nextStart(Instant asOf) {
    return credits.nextAppointment(asOf).orElseThrow().start();
  }

  /**
   * Without an appointment, because late cancellations forfeited every credit, the purchase time.
   */
  private Instant pastOrderKey(Instant asOf) {
    return cancelledAt.orElseGet(() ->
        credits.pastAppointments(asOf).stream()
            .findFirst()
            .map(Appointment::start)
            .orElse(purchasedAt));
  }
}
