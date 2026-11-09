package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.SessionCount;

/**
 * A booked appointment at a given time (glossary, spec 04 decision 21): {@code BOOKED} until its start, then
 * {@code CONSUMED}; upcoming until its end, past from its end on; and the credits the booked appointments use.
 * Instants are in UTC; Zurich is UTC+1 in November.
 */
class AppointmentTest {

    private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

    /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
    private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");
    private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

    private static final Appointment TEN_TO_ELEVEN = new Appointment(new AppointmentId(UUID.randomUUID()), CASE, START,
            END);

    private static Appointment appointment(String start, String end) {
        return new Appointment(new AppointmentId(UUID.randomUUID()), CASE, Instant.parse(start), Instant.parse(end));
    }

    @Test
    void isBookedUntilItsStartAndConsumedFromItsStartOn() {
        assertThat(TEN_TO_ELEVEN.statusAt(START.minusSeconds(1))).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(TEN_TO_ELEVEN.statusAt(START)).isEqualTo(AppointmentStatus.CONSUMED);
        assertThat(TEN_TO_ELEVEN.statusAt(END)).isEqualTo(AppointmentStatus.CONSUMED);
        assertThat(TEN_TO_ELEVEN.statusAt(END.plusSeconds(86_400))).isEqualTo(AppointmentStatus.CONSUMED);
    }

    @Test
    void isUpcomingUntilItsEndAlsoWhileItRuns() {
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(START.minusSeconds(1))).isTrue();
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(START)).isTrue();
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(END.minusSeconds(1))).isTrue();
        assertThat(TEN_TO_ELEVEN.isUpcomingAt(END)).isFalse();
    }

    @Test
    void asTheCustomerSeesItCarriesTheStatusAtThatTime() {
        assertThat(TEN_TO_ELEVEN.at(START.minusSeconds(1)))
                .isEqualTo(new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.BOOKED));
        assertThat(TEN_TO_ELEVEN.at(START))
                .isEqualTo(new CustomerAppointment(TEN_TO_ELEVEN.id(), START, END, AppointmentStatus.CONSUMED));
    }

    @Test
    void theUpcomingOnesAreTheOnesNotEndedSoonestFirstARunningOneConsumed() {
        Appointment past = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
        Appointment running = appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");
        Appointment later = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
        Appointment soon = appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");
        Instant asOf = Instant.parse("2026-11-17T09:30:00Z");

        assertThat(Appointment.upcoming(List.of(later, past, soon, running), asOf)).containsExactly(
                running.at(asOf), soon.at(asOf), later.at(asOf));
        assertThat(Appointment.upcoming(List.of(later, past, soon, running), asOf))
                .extracting(CustomerAppointment::status)
                .containsExactly(AppointmentStatus.CONSUMED, AppointmentStatus.BOOKED, AppointmentStatus.BOOKED);
    }

    @Test
    void thePastOnesAreTheOnesEndedNewestFirstAllConsumed() {
        Appointment first = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");
        Appointment second = appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");
        Appointment upcoming = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
        Instant asOf = Instant.parse("2026-11-17T10:00:00Z");

        assertThat(Appointment.past(List.of(first, upcoming, second), asOf)).containsExactly(second.at(asOf),
                first.at(asOf));
        assertThat(Appointment.past(List.of(first, upcoming, second), asOf)).extracting(CustomerAppointment::status)
                .containsOnly(AppointmentStatus.CONSUMED);
    }

    @Test
    void anAppointmentIsUpcomingOrPastNeverBoth() {
        Appointment appointment = TEN_TO_ELEVEN;
        for (Instant asOf : List.of(START.minusSeconds(1), START, END.minusSeconds(1), END)) {
            assertThat(Appointment.upcoming(List.of(appointment), asOf).size()
                    + Appointment.past(List.of(appointment), asOf).size()).as("at " + asOf).isOne();
        }
    }

    @Test
    void theOpenCreditsAreTheSessionsLessTheBookedAppointmentsConsumedOrNot() {
        assertThat(Appointment.openCredits(new SessionCount(3), 0)).isEqualTo(3);
        assertThat(Appointment.openCredits(new SessionCount(3), 2)).isEqualTo(1);
        assertThat(Appointment.openCredits(new SessionCount(3), 3)).isZero();
        assertThat(Appointment.openCredits(new SessionCount(5), 1)).isEqualTo(4);
    }

    @Test
    void endsAfterItStarts() {
        AppointmentId id = new AppointmentId(UUID.randomUUID());

        assertThatIllegalArgumentException().isThrownBy(() -> new Appointment(id, CASE, END, START))
                .withMessage("An appointment ends after it starts: " + END + " to " + START + ".");
        assertThatIllegalArgumentException().isThrownBy(() -> new Appointment(id, CASE, START, START));
    }

    @Test
    void needsAllItsFields() {
        AppointmentId id = new AppointmentId(UUID.randomUUID());

        assertThatNullPointerException().isThrownBy(() -> new Appointment(null, CASE, START, END)).withMessage("id");
        assertThatNullPointerException().isThrownBy(() -> new Appointment(id, null, START, END))
                .withMessage("consultationCaseId");
        assertThatNullPointerException().isThrownBy(() -> new Appointment(id, CASE, null, END)).withMessage("start");
        assertThatNullPointerException().isThrownBy(() -> new Appointment(id, CASE, START, null)).withMessage("end");
    }
}
