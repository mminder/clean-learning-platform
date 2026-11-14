package com.cleanlearning.domain.consultation.read;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * An appointment as its customer sees it at a given time (UC-05): its start and end, and its status
 * then, which {@link Appointment#statusAt} derives. It shows nothing of the expert's other
 * appointments.
 */
public record CustomerAppointment(
    AppointmentId id, Instant start, Instant end, AppointmentStatus status) {

  public CustomerAppointment {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    Objects.requireNonNull(status, "status");
  }

  /** The appointment as its customer sees it at {@code asOf}. */
  public static CustomerAppointment of(Appointment appointment, Instant asOf) {
    return new CustomerAppointment(
        appointment.id(), appointment.start(), appointment.end(), appointment.statusAt(asOf));
  }

  /**
   * The upcoming appointments at {@code asOf}, the ones that haven't ended, soonest first, each
   * with its status: a running one is {@code CONSUMED}.
   */
  public static List<CustomerAppointment> upcoming(List<Appointment> appointments, Instant asOf) {
    return Appointment.sortedUpcoming(appointments, asOf).stream()
        .map(appointment -> of(appointment, asOf))
        .toList();
  }

  /**
   * The past appointments at {@code asOf}, the ones that have ended, newest first, all {@code
   * CONSUMED}.
   */
  public static List<CustomerAppointment> past(List<Appointment> appointments, Instant asOf) {
    return Appointment.sortedPast(appointments, asOf).stream()
        .map(appointment -> of(appointment, asOf))
        .toList();
  }
}
