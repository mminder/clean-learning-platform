package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import java.time.Instant;
import java.util.Objects;

/**
 * An appointment as its customer sees it at a given time (UC-05): its start and end, and its status
 * then, which {@link Appointment#at} derives. It shows nothing of the expert's other appointments.
 */
public record CustomerAppointment(
    AppointmentId id, Instant start, Instant end, AppointmentStatus status) {

  public CustomerAppointment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    Objects.requireNonNull(status, "status");
  }
}
