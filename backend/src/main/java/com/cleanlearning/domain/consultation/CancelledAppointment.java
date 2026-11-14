package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.outcome.AppointmentCancellation;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import java.time.Instant;
import java.util.Objects;

/**
 * {@link AppointmentCancellation} is the decision that made it. The customer's pages don't show it,
 * only its forfeited credit in the counts.
 */
public record CancelledAppointment(
    AppointmentId id, Instant start, Instant end, boolean creditRefunded) {

  public CancelledAppointment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (!start.isBefore(end)) {
      throw new IllegalArgumentException(
          "An appointment ends after it starts: " + start + " to " + end + ".");
    }
  }

  public boolean isCreditForfeited() {
    return !creditRefunded;
  }
}
