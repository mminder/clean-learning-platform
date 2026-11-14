package com.cleanlearning.domain.consultation.view;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import java.time.Instant;
import java.util.Objects;

public record CustomerAppointment(
    AppointmentId id, Instant start, Instant end, AppointmentStatus status) {

  public CustomerAppointment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    Objects.requireNonNull(status, "status");
  }

  public static CustomerAppointment of(Appointment appointment, Instant asOf) {
    return new CustomerAppointment(
        appointment.id(), appointment.start(), appointment.end(), appointment.statusAt(asOf));
  }
}
