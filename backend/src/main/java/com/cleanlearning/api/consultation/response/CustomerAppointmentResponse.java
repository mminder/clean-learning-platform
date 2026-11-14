package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.view.CustomerAppointment;
import java.time.Instant;
import java.util.UUID;

/**
 * An appointment as its customer sees it (UC-05, UC-06): its start and end, instants, and its
 * status at the time of the answer, {@code BOOKED} before its start and {@code CONSUMED} from its
 * start on, computed by the server.
 */
public record CustomerAppointmentResponse(
    UUID id, Instant start, Instant end, AppointmentStatus status) {

  public static CustomerAppointmentResponse from(CustomerAppointment appointment) {
    return new CustomerAppointmentResponse(
        appointment.id().value(), appointment.start(), appointment.end(), appointment.status());
  }
}
