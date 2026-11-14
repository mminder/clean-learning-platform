package com.cleanlearning.domain.consultation.change;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import java.util.Objects;

/**
 * What cancelling an appointment writes (UC-07 step 4), and what the case keeps of a cancelled one:
 * the appointment, now {@code CANCELLED}, and whether its credit was refunded, in time, or
 * forfeited, late (glossary).
 *
 * @param creditRefunded {@code true} if it was cancelled before its free-cancellation time
 */
public record AppointmentCancellation(AppointmentId appointmentId, boolean creditRefunded) {

  public AppointmentCancellation {
    Objects.requireNonNull(appointmentId, "appointmentId");
  }
}
