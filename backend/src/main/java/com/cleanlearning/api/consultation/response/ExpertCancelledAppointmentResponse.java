package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.CancelledAppointment;
import java.time.Instant;
import java.util.UUID;

public record ExpertCancelledAppointmentResponse(
    UUID id, Instant start, Instant end, boolean creditRefunded) {

  public static ExpertCancelledAppointmentResponse from(CancelledAppointment appointment) {
    return new ExpertCancelledAppointmentResponse(
        appointment.id().value(),
        appointment.start(),
        appointment.end(),
        appointment.creditRefunded());
  }
}
