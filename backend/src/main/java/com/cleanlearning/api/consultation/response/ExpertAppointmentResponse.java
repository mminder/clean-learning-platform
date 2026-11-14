package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.view.ExpertAppointment;
import java.time.Instant;
import java.util.UUID;

public record ExpertAppointmentResponse(
    UUID id, Instant start, Instant end, AppointmentStatus status) {

  public static ExpertAppointmentResponse from(ExpertAppointment appointment) {
    return new ExpertAppointmentResponse(
        appointment.id().value(), appointment.start(), appointment.end(), appointment.status());
  }
}
