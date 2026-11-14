package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import java.time.Instant;
import java.util.Objects;

/** A booked appointment as its expert sees it at {@code asOf}. */
public record ExpertAppointment(
    AppointmentId id, Instant start, Instant end, AppointmentStatus status) {

  public ExpertAppointment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    Objects.requireNonNull(status, "status");
  }

  public static ExpertAppointment of(Appointment appointment, Instant asOf) {
    return new ExpertAppointment(
        appointment.id(), appointment.start(), appointment.end(), appointment.statusAt(asOf));
  }
}
