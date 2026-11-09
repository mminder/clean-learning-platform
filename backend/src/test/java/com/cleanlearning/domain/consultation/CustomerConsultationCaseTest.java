package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;

/**
 * A case as its customer sees it at a given time (UC-05's case detail): the credits, which every booked appointment
 * uses, consumed or not (spec 04, decision 15), whether the customer can book, and the upcoming and past appointments
 * (decision 21). Instants are in UTC; Zurich is UTC+1 in November.
 */
class CustomerConsultationCaseTest {

    private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

    private static final AssignedExpert EXPERT_2 = new AssignedExpert(
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000202")), new DisplayName("Expert 2"),
            "Leadership coach.");

    /** Mon 16 Nov 2026, 10:00–11:00 in Zurich. */
    private static final Appointment MONDAY = appointment("2026-11-16T09:00:00Z", "2026-11-16T10:00:00Z");

    /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
    private static final Appointment TUESDAY = appointment("2026-11-17T09:00:00Z", "2026-11-17T10:00:00Z");

    /** Wed 18 Nov 2026, 10:00–11:00 in Zurich. */
    private static final Appointment WEDNESDAY = appointment("2026-11-18T09:00:00Z", "2026-11-18T10:00:00Z");

    /** Mon 9 Nov 2026, 15:00 in Zurich. */
    private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

    private static Appointment appointment(String start, String end) {
        return new Appointment(new AppointmentId(UUID.randomUUID()), CASE, Instant.parse(start), Instant.parse(end));
    }

    private static CustomerConsultationCase withSessions(int sessionCount) {
        return withSessions(sessionCount, Optional.empty(), List.of(), NOW);
    }

    private static CustomerConsultationCase withSessions(int sessionCount, Optional<AssignedExpert> expert,
            List<Appointment> appointments, Instant asOf) {
        return new CustomerConsultationCase(CASE, new OfferingName("Career Coaching"), Optional.empty(),
                new PurchaseTerms(Money.chf("450.00"), new SessionCount(sessionCount), new SessionDuration(60),
                        new CancellationDeadline(24)),
                Instant.parse("2026-11-09T14:03:12.345Z"), expert, appointments, asOf);
    }

    private static CustomerConsultationCase withAppointments(List<Appointment> appointments, Instant asOf) {
        return withSessions(3, Optional.of(EXPERT_2), appointments, asOf);
    }

    @Test
    void totalCreditsAreTheSessionCount() {
        assertThat(withSessions(3).totalCredits()).isEqualTo(3);
        assertThat(withSessions(1).totalCredits()).isEqualTo(1);
    }

    @Test
    void withoutAppointmentsEveryCreditIsOpenAndTheCustomerCanBook() {
        assertThat(withSessions(3).openCredits()).isEqualTo(3);
        assertThat(withSessions(5).openCredits()).isEqualTo(5);
        assertThat(withSessions(3).canBook()).isTrue();
        assertThat(withSessions(3).expert()).isEmpty();
    }

    @Test
    void everyBookedAppointmentUsesACreditConsumedOrNot() {
        // At Tuesday 10:30: Monday's is past, Tuesday's runs, Wednesday's is ahead.
        CustomerConsultationCase consultationCase = withAppointments(List.of(MONDAY, TUESDAY),
                Instant.parse("2026-11-17T09:30:00Z"));

        assertThat(consultationCase.openCredits()).isEqualTo(1);
        assertThat(consultationCase.canBook()).isTrue();
    }

    @Test
    void withoutAnOpenCreditTheCustomerCantBook() {
        CustomerConsultationCase consultationCase = withAppointments(List.of(MONDAY, TUESDAY, WEDNESDAY), NOW);

        assertThat(consultationCase.openCredits()).isZero();
        assertThat(consultationCase.canBook()).isFalse();
    }

    @Test
    void beforeItsStartAnAppointmentIsUpcomingAndBooked() {
        CustomerConsultationCase consultationCase = withAppointments(List.of(WEDNESDAY, TUESDAY), NOW);

        assertThat(consultationCase.upcomingAppointments()).containsExactly(
                new CustomerAppointment(TUESDAY.id(), TUESDAY.start(), TUESDAY.end(), AppointmentStatus.BOOKED),
                new CustomerAppointment(WEDNESDAY.id(), WEDNESDAY.start(), WEDNESDAY.end(), AppointmentStatus.BOOKED));
        assertThat(consultationCase.pastAppointments()).isEmpty();
    }

    @Test
    void fromItsStartAnAppointmentIsConsumedAndStillUpcoming() {
        CustomerConsultationCase atTheStart = withAppointments(List.of(TUESDAY), TUESDAY.start());
        CustomerConsultationCase whileItRuns = withAppointments(List.of(TUESDAY),
                Instant.parse("2026-11-17T09:30:00Z"));

        for (CustomerConsultationCase consultationCase : List.of(atTheStart, whileItRuns)) {
            assertThat(consultationCase.upcomingAppointments()).containsExactly(
                    new CustomerAppointment(TUESDAY.id(), TUESDAY.start(), TUESDAY.end(), AppointmentStatus.CONSUMED));
            assertThat(consultationCase.pastAppointments()).isEmpty();
            assertThat(consultationCase.openCredits()).isEqualTo(2);
        }
    }

    @Test
    void fromItsEndAnAppointmentIsPastAndConsumed() {
        CustomerConsultationCase consultationCase = withAppointments(List.of(TUESDAY), TUESDAY.end());

        assertThat(consultationCase.upcomingAppointments()).isEmpty();
        assertThat(consultationCase.pastAppointments()).containsExactly(
                new CustomerAppointment(TUESDAY.id(), TUESDAY.start(), TUESDAY.end(), AppointmentStatus.CONSUMED));
        assertThat(consultationCase.openCredits()).isEqualTo(2);
    }

    @Test
    void upcomingSoonestFirstAndPastNewestFirst() {
        Appointment friday = appointment("2026-11-20T09:00:00Z", "2026-11-20T10:00:00Z");
        CustomerConsultationCase consultationCase = withSessions(5, Optional.of(EXPERT_2),
                List.of(TUESDAY, friday, MONDAY, WEDNESDAY), Instant.parse("2026-11-18T12:00:00Z"));

        assertThat(consultationCase.upcomingAppointments()).extracting(CustomerAppointment::id)
                .containsExactly(friday.id());
        assertThat(consultationCase.pastAppointments()).extracting(CustomerAppointment::id)
                .containsExactly(WEDNESDAY.id(), TUESDAY.id(), MONDAY.id());
    }

    @Test
    void showsTheAssignedExpert() {
        assertThat(withAppointments(List.of(TUESDAY), NOW).expert()).contains(EXPERT_2);
    }
}
