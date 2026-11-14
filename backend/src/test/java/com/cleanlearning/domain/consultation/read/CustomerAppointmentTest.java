package com.cleanlearning.domain.consultation.read;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * An appointment as its customer sees it at a given time (spec 04 decision 21): with its status
 * then, and upcoming until its end, past from its end on. Instants are in UTC.
 */
class CustomerAppointmentTest {

  private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  private static final Appointment TEN_TO_ELEVEN =
      new Appointment(new AppointmentId(UUID.randomUUID()), CASE, START, END);

  private static Appointment appointment(String start, String end) {
    return new Appointment(
        new AppointmentId(UUID.randomUUID()), CASE, Instant.parse(start), Instant.parse(end));
  }

  @Test
  void carriesTheStatusAtThatTime() {
    assertThat(CustomerAppointment.of(TEN_TO_ELEVEN, START.minusSeconds(1)))
        .isEqualTo(
            new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.BOOKED));
    assertThat(CustomerAppointment.of(TEN_TO_ELEVEN, START))
        .isEqualTo(
            new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.CONSUMED));
  }

  @Test
  void theUpcomingOnesAreTheOnesNotEndedSoonestFirstARunningOneConsumed() {
    Appointment past = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
    Appointment running = appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");
    Appointment later = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
    Appointment soon = appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");
    Instant asOf = Instant.parse("2026-11-17T09:30:00Z");

    assertThat(CustomerAppointment.upcoming(List.of(later, past, soon, running), asOf))
        .containsExactly(
            CustomerAppointment.of(running, asOf),
            CustomerAppointment.of(soon, asOf),
            CustomerAppointment.of(later, asOf));
    assertThat(CustomerAppointment.upcoming(List.of(later, past, soon, running), asOf))
        .extracting(CustomerAppointment::status)
        .containsExactly(
            AppointmentStatus.CONSUMED, AppointmentStatus.BOOKED, AppointmentStatus.BOOKED);
  }

  @Test
  void thePastOnesAreTheOnesEndedNewestFirstAllConsumed() {
    Appointment first = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
    Appointment second = appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");
    Appointment upcoming = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
    Instant asOf = Instant.parse("2026-11-17T10:00:00Z");

    assertThat(CustomerAppointment.past(List.of(first, upcoming, second), asOf))
        .containsExactly(CustomerAppointment.of(second, asOf), CustomerAppointment.of(first, asOf));
    assertThat(CustomerAppointment.past(List.of(first, upcoming, second), asOf))
        .extracting(CustomerAppointment::status)
        .containsOnly(AppointmentStatus.CONSUMED);
  }

  @Test
  void anAppointmentIsUpcomingOrPastNeverBoth() {
    for (Instant asOf : List.of(START.minusSeconds(1), START, END.minusSeconds(1), END)) {
      assertThat(
              CustomerAppointment.upcoming(List.of(TEN_TO_ELEVEN), asOf).size()
                  + CustomerAppointment.past(List.of(TEN_TO_ELEVEN), asOf).size())
          .as("at " + asOf)
          .isOne();
    }
  }
}
