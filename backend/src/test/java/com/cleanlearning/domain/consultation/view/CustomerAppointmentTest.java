package com.cleanlearning.domain.consultation.view;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * An appointment as its customer sees it at a given time (glossary, spec 04 decision 21): with its
 * status then. Instants are in UTC.
 */
class CustomerAppointmentTest {

  private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  private static final Appointment TEN_TO_ELEVEN =
      new Appointment(new AppointmentId(UUID.randomUUID()), CASE, START, END);

  @Test
  void asTheCustomerSeesItCarriesTheStatusAtThatTime() {
    assertThat(CustomerAppointment.of(TEN_TO_ELEVEN, START.minusSeconds(1)))
        .isEqualTo(
            new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.BOOKED));
    assertThat(CustomerAppointment.of(TEN_TO_ELEVEN, START))
        .isEqualTo(
            new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.CONSUMED));
  }
}
