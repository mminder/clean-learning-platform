package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.view.CustomerAppointment;
import java.time.Instant;
import java.util.UUID;

public record CustomerAppointmentResponse(
    UUID id, Instant start, Instant end, AppointmentStatus status) {

  public static CustomerAppointmentResponse from(CustomerAppointment appointment) {
    return new CustomerAppointmentResponse(
        appointment.id().value(), appointment.start(), appointment.end(), appointment.status());
  }
}
