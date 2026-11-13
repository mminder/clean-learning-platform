package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.availability.value.TimeRange;
import com.cleanlearning.domain.consultation.value.BookableSlot;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.EligibleExpert;
import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.shared.value.Money;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;

/**
 * UC-06's rules for a booking, as the plan's booking table in _docs/tasks/04-book-consultation-appointment/README.md
 * lists them: the open credit first, then the expert, then the slot. "Now" is Mon 9 Nov 2026, 15:00 in Zurich, and
 * instants are written with their Zurich time, as in spec 04.
 */
class CaseForBookingTest {

    private static final ConsultationCaseId CASE = new ConsultationCaseId(UUID.randomUUID());

    private static final EligibleExpert EXPERT_1 = new EligibleExpert(
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000201")), new DisplayName("Expert 1"),
            "Career coach.");

    private static final EligibleExpert EXPERT_2 = new EligibleExpert(
            new UserId(UUID.fromString("00000000-0000-0000-0000-000000000202")), new DisplayName("Expert 2"),
            "Leadership coach.");

    private static final UserId SOMEONE_ELSE = new UserId(UUID.randomUUID());

    /** Career Coaching's 3 sessions: 60 minutes, free cancellation until 24 hours before. */
    private static final PurchaseTerms THREE_SESSIONS = new PurchaseTerms(Money.chf("450.00"), new SessionCount(3),
            new SessionDuration(60), new CancellationDeadline(24));

    private static final Instant NOW = zurich("2026-11-09T15:00");

    /** Two sessions of 60 minutes, free cancellation until 48 hours before: longer than the 24 hours' notice. */
    private static final PurchaseTerms FORTY_EIGHT_HOURS = new PurchaseTerms(Money.chf("300.00"), new SessionCount(2),
            new SessionDuration(60), new CancellationDeadline(48));

    /** The expert is available on Tue 17 Nov 2026, 09:00–12:00. */
    private static final SlotFinder SEVENTEENTH_NINE_TO_TWELVE = new SlotFinder(
            new Availability(new TreeSet<>(List.of(LocalDate.parse("2026-11-17"))),
                    List.of(new TimeRange(LocalTime.parse("09:00"), LocalTime.parse("12:00")))),
            List.of());

    /** The expert is available on Wed 11 Nov 2026, 14:00–17:00: after the notice, but within 48 hours of now. */
    private static final SlotFinder ELEVENTH_TWO_TO_FIVE = new SlotFinder(
            new Availability(new TreeSet<>(List.of(LocalDate.parse("2026-11-11"))),
                    List.of(new TimeRange(LocalTime.parse("14:00"), LocalTime.parse("17:00")))),
            List.of());

    private static final String NO_OPEN_CREDITS = "This case has no open credits.";
    private static final String EXPERT_CHANGED = "This case already has an expert. Please choose a time with them.";
    private static final String EXPERT_NOT_ELIGIBLE =
            "This expert is no longer available for this offering. Please choose another.";
    private static final String SLOT_NOT_AVAILABLE = "This time is no longer available.";

    private static Instant zurich(String localDateTime) {
        return LocalDateTime.parse(localDateTime).atZone(BusinessTimeZone.ZONE).toInstant();
    }

    /** A slot from {@code start} to {@code end}, free to cancel until {@code freeUntil}, all in Zurich. */
    private static BookableSlot slot(String start, String end, String freeUntil) {
        return new BookableSlot(zurich(start), zurich(end), Optional.of(zurich(freeUntil)));
    }

    /** A case of three sessions without an expert, with these booked appointments, Expert 1 and 2 eligible. */
    private static CaseForBooking withoutExpert(int bookedAppointments) {
        return new CaseForBooking(CASE, THREE_SESSIONS, Optional.empty(), bookedAppointments, 0,
                List.of(EXPERT_1, EXPERT_2));
    }

    private static CaseForBooking withExpert(UserId expert, int bookedAppointments, List<EligibleExpert> eligible) {
        return new CaseForBooking(CASE, THREE_SESSIONS, Optional.of(expert), bookedAppointments, 0, eligible);
    }

    private static void assertRefused(Runnable booking, String code, String message) {
        BusinessRuleException refused = catchThrowableOfType(BusinessRuleException.class, booking::run);
        assertThat(refused).as("refused").isNotNull();
        assertThat(refused.code()).isEqualTo(code);
        assertThat(refused).hasMessage(message);
    }

    @Test
    void openCreditsAreTheSessionsLessTheBookedAppointments() {
        assertThat(withoutExpert(0).openCredits()).isEqualTo(3);
        assertThat(withoutExpert(2).openCredits()).isEqualTo(1);
        assertThat(withoutExpert(3).openCredits()).isZero();
    }

    /** Spec 05: a forfeited credit is used, as a booked one is; one cancelled in time is neither. */
    @Test
    void openCreditsAlsoLackTheForfeitedCredits() {
        assertThat(new CaseForBooking(CASE, THREE_SESSIONS, Optional.of(EXPERT_2.id()), 1, 1, List.of(EXPERT_2))
                .openCredits()).isOne();
        assertThat(new CaseForBooking(CASE, THREE_SESSIONS, Optional.empty(), 0, 3, List.of(EXPERT_2)).openCredits())
                .isZero();
    }

    @Test
    void anOpenCreditIsRequiredAndAForfeitedOneIsNone() {
        new CaseForBooking(CASE, THREE_SESSIONS, Optional.of(EXPERT_2.id()), 1, 1, List.of(EXPERT_2))
                .requireOpenCredit();

        assertRefused(() -> new CaseForBooking(CASE, THREE_SESSIONS, Optional.of(EXPERT_2.id()), 2, 1,
                List.of(EXPERT_2)).book(EXPERT_2.id(), zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE, NOW),
                CaseForBooking.NO_OPEN_CREDITS, NO_OPEN_CREDITS);
    }

    @Test
    void anOpenCreditIsRequired() {
        withoutExpert(2).requireOpenCredit();

        assertRefused(() -> withoutExpert(3).requireOpenCredit(), CaseForBooking.NO_OPEN_CREDITS, NO_OPEN_CREDITS);
    }

    @Test
    void withoutAnExpertTheNamedOneMustBeEligible() {
        assertThat(withoutExpert(0).expertFor(EXPERT_2.id())).isEqualTo(EXPERT_2.id());
        assertThat(withoutExpert(0).expertFor(EXPERT_1.id())).isEqualTo(EXPERT_1.id());

        assertRefused(() -> withoutExpert(0).expertFor(SOMEONE_ELSE), CaseForBooking.EXPERT_NOT_ELIGIBLE,
                EXPERT_NOT_ELIGIBLE);
        assertRefused(() -> new CaseForBooking(CASE, THREE_SESSIONS, Optional.empty(), 0, 0, List.of())
                .expertFor(EXPERT_2.id()), CaseForBooking.EXPERT_NOT_ELIGIBLE, EXPERT_NOT_ELIGIBLE);
    }

    @Test
    void withAnExpertTheNamedOneMustBeTheCases() {
        assertThat(withExpert(EXPERT_2.id(), 1, List.of(EXPERT_1, EXPERT_2)).expertFor(EXPERT_2.id()))
                .isEqualTo(EXPERT_2.id());

        assertRefused(() -> withExpert(EXPERT_2.id(), 1, List.of(EXPERT_1, EXPERT_2)).expertFor(EXPERT_1.id()),
                CaseForBooking.EXPERT_CHANGED, EXPERT_CHANGED);
        assertRefused(() -> withExpert(EXPERT_2.id(), 1, List.of(EXPERT_1, EXPERT_2)).expertFor(SOMEONE_ELSE),
                CaseForBooking.EXPERT_CHANGED, EXPERT_CHANGED);
    }

    @Test
    void theCasesExpertStaysAfterTheyStopBeingEligible() {
        assertThat(withExpert(EXPERT_2.id(), 1, List.of(EXPERT_1)).expertFor(EXPERT_2.id())).isEqualTo(EXPERT_2.id());
        assertThat(withExpert(EXPERT_2.id(), 1, List.of()).expertFor(EXPERT_2.id())).isEqualTo(EXPERT_2.id());
    }

    @Test
    void theSlotsHaveTheSessionsEndAndTheFreeCancellationTime() {
        assertThat(withoutExpert(0).bookableSlots(SEVENTEENTH_NINE_TO_TWELVE, NOW)).containsExactly(
                slot("2026-11-17T09:00", "2026-11-17T10:00", "2026-11-16T09:00"),
                slot("2026-11-17T09:30", "2026-11-17T10:30", "2026-11-16T09:30"),
                slot("2026-11-17T10:00", "2026-11-17T11:00", "2026-11-16T10:00"),
                slot("2026-11-17T10:30", "2026-11-17T11:30", "2026-11-16T10:30"),
                slot("2026-11-17T11:00", "2026-11-17T12:00", "2026-11-16T11:00"));
    }

    @Test
    void theSlotsFollowTheCasesTermsNotTheOfferings() {
        CaseForBooking ninetyMinutesFortyEightHours = new CaseForBooking(CASE, new PurchaseTerms(Money.chf("300.00"),
                new SessionCount(2), new SessionDuration(90), new CancellationDeadline(48)), Optional.empty(), 0,
                0, List.of(EXPERT_2));

        assertThat(ninetyMinutesFortyEightHours.bookableSlots(SEVENTEENTH_NINE_TO_TWELVE, NOW)).containsExactly(
                slot("2026-11-17T09:00", "2026-11-17T10:30", "2026-11-15T09:00"),
                slot("2026-11-17T09:30", "2026-11-17T11:00", "2026-11-15T09:30"),
                slot("2026-11-17T10:00", "2026-11-17T11:30", "2026-11-15T10:00"),
                slot("2026-11-17T10:30", "2026-11-17T12:00", "2026-11-15T10:30"));
    }

    @Test
    void aSlotWithinTheCancellationDeadlineHasNoFreeCancellationTime() {
        CaseForBooking consultationCase = new CaseForBooking(CASE, FORTY_EIGHT_HOURS, Optional.empty(), 0, 0,
                List.of(EXPERT_2));

        // 48 hours before 14:00, 14:30 and 15:00 is before now or now: cancelling is free only before it (D-11).
        assertThat(consultationCase.bookableSlots(ELEVENTH_TWO_TO_FIVE, NOW)).containsExactly(
                new BookableSlot(zurich("2026-11-11T14:00"), zurich("2026-11-11T15:00"), Optional.empty()),
                new BookableSlot(zurich("2026-11-11T14:30"), zurich("2026-11-11T15:30"), Optional.empty()),
                new BookableSlot(zurich("2026-11-11T15:00"), zurich("2026-11-11T16:00"), Optional.empty()),
                slot("2026-11-11T15:30", "2026-11-11T16:30", "2026-11-09T15:30"),
                slot("2026-11-11T16:00", "2026-11-11T17:00", "2026-11-09T16:00"));
    }

    @Test
    void freeCancellationEndingNowHasPassed() {
        CaseForBooking consultationCase = new CaseForBooking(CASE, FORTY_EIGHT_HOURS, Optional.empty(), 0, 0,
                List.of(EXPERT_2));

        // 15:00's free cancellation ends at now; a second before now, it's still ahead.
        assertThat(consultationCase.bookableSlots(ELEVENTH_TWO_TO_FIVE, NOW))
                .extracting(BookableSlot::freeCancellationUntil)
                .containsExactly(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.of(zurich("2026-11-09T15:30")), Optional.of(zurich("2026-11-09T16:00")));
        assertThat(consultationCase.bookableSlots(ELEVENTH_TWO_TO_FIVE, NOW.minusSeconds(1)))
                .extracting(BookableSlot::freeCancellationUntil)
                .containsExactly(Optional.empty(), Optional.empty(), Optional.of(NOW),
                        Optional.of(zurich("2026-11-09T15:30")), Optional.of(zurich("2026-11-09T16:00")));
    }

    @Test
    void theSlotsLeaveOutBusyTime() {
        SlotFinder busyTenToEleven = new SlotFinder(SEVENTEENTH_NINE_TO_TWELVE.availability(),
                List.of(new BusyTime(zurich("2026-11-17T10:00"), zurich("2026-11-17T11:00"))));

        assertThat(withoutExpert(0).bookableSlots(busyTenToEleven, NOW)).extracting(BookableSlot::start)
                .containsExactly(zurich("2026-11-17T09:00"), zurich("2026-11-17T11:00"));
    }

    @Test
    void theFirstBookingAssignsTheExpert() {
        Booking booking = withoutExpert(0).book(EXPERT_2.id(), zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE,
                NOW);

        assertThat(booking.expertId()).isEqualTo(EXPERT_2.id());
        assertThat(booking.assignsExpert()).isTrue();
        assertThat(booking.appointment().id()).isNotNull();
        assertThat(booking.appointment().consultationCaseId()).isEqualTo(CASE);
        assertThat(booking.appointment().start()).isEqualTo(zurich("2026-11-17T10:00"));
        assertThat(booking.appointment().end()).isEqualTo(zurich("2026-11-17T11:00"));
    }

    @Test
    void aLaterBookingKeepsTheCasesExpert() {
        Booking booking = withExpert(EXPERT_2.id(), 2, List.of(EXPERT_1)).book(EXPERT_2.id(),
                zurich("2026-11-17T11:00"), SEVENTEENTH_NINE_TO_TWELVE, NOW);

        assertThat(booking.expertId()).isEqualTo(EXPERT_2.id());
        assertThat(booking.assignsExpert()).isFalse();
        assertThat(booking.appointment().end()).isEqualTo(zurich("2026-11-17T12:00"));
    }

    @Test
    void eachBookingIsANewAppointment() {
        CaseForBooking consultationCase = withoutExpert(0);
        Instant start = zurich("2026-11-17T10:00");

        assertThat(consultationCase.book(EXPERT_2.id(), start, SEVENTEENTH_NINE_TO_TWELVE, NOW).appointment().id())
                .isNotEqualTo(consultationCase.book(EXPERT_2.id(), start, SEVENTEENTH_NINE_TO_TWELVE, NOW)
                        .appointment().id());
    }

    @Test
    void anExpertBooksThemself() {
        // Expert 1 is also a customer (D-06): on their own case, they're one of the eligible experts like any other.
        Booking booking = withoutExpert(0).book(EXPERT_1.id(), zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE,
                NOW);

        assertThat(booking.expertId()).isEqualTo(EXPERT_1.id());
    }

    @Test
    void refusesWithoutAnOpenCredit() {
        assertRefused(() -> withExpert(EXPERT_2.id(), 3, List.of(EXPERT_2)).book(EXPERT_2.id(),
                zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE, NOW), CaseForBooking.NO_OPEN_CREDITS,
                NO_OPEN_CREDITS);
    }

    @Test
    void checksTheCreditsBeforeTheExpert() {
        // A second booking on the last credit is NO_OPEN_CREDITS, whichever expert it names (plan, booking).
        assertRefused(() -> withExpert(EXPERT_2.id(), 3, List.of(EXPERT_2)).book(EXPERT_1.id(),
                zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE, NOW), CaseForBooking.NO_OPEN_CREDITS,
                NO_OPEN_CREDITS);
        assertRefused(() -> new CaseForBooking(CASE, THREE_SESSIONS, Optional.empty(), 3, 0, List.of())
                .book(SOMEONE_ELSE, zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE, NOW),
                CaseForBooking.NO_OPEN_CREDITS, NO_OPEN_CREDITS);
    }

    @Test
    void refusesAnotherExpertThanTheCases() {
        assertRefused(() -> withExpert(EXPERT_2.id(), 1, List.of(EXPERT_1, EXPERT_2)).book(EXPERT_1.id(),
                zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE, NOW), CaseForBooking.EXPERT_CHANGED,
                EXPERT_CHANGED);
    }

    @Test
    void refusesAnExpertNoLongerEligibleOnTheFirstBooking() {
        assertRefused(() -> new CaseForBooking(CASE, THREE_SESSIONS, Optional.empty(), 0, 0, List.of(EXPERT_1))
                .book(EXPERT_2.id(), zurich("2026-11-17T10:00"), SEVENTEENTH_NINE_TO_TWELVE, NOW),
                CaseForBooking.EXPERT_NOT_ELIGIBLE, EXPERT_NOT_ELIGIBLE);
    }

    @Test
    void checksTheExpertBeforeTheSlot() {
        assertRefused(() -> withoutExpert(0).book(SOMEONE_ELSE, zurich("2026-11-17T15:00"),
                SEVENTEENTH_NINE_TO_TWELVE, NOW), CaseForBooking.EXPERT_NOT_ELIGIBLE, EXPERT_NOT_ELIGIBLE);
    }

    @Test
    void refusesAStartThatIsNotABookableSlot() {
        CaseForBooking consultationCase = withoutExpert(0);

        // outside the availability, off the grid, a second off, too late to fit, busy
        for (Instant start : List.of(zurich("2026-11-17T13:00"), zurich("2026-11-17T10:15"),
                zurich("2026-11-17T10:00").plusSeconds(1), zurich("2026-11-17T11:30"), zurich("2026-11-18T10:00"))) {
            assertRefused(() -> consultationCase.book(EXPERT_2.id(), start, SEVENTEENTH_NINE_TO_TWELVE, NOW),
                    CaseForBooking.SLOT_NOT_AVAILABLE, SLOT_NOT_AVAILABLE);
        }
        SlotFinder busyTenToEleven = new SlotFinder(SEVENTEENTH_NINE_TO_TWELVE.availability(),
                List.of(new BusyTime(zurich("2026-11-17T10:00"), zurich("2026-11-17T11:00"))));
        assertRefused(() -> consultationCase.book(EXPERT_2.id(), zurich("2026-11-17T10:30"), busyTenToEleven, NOW),
                CaseForBooking.SLOT_NOT_AVAILABLE, SLOT_NOT_AVAILABLE);
    }

    @Test
    void refusesAStartWithinTheMinimumNotice() {
        Instant now = zurich("2026-11-16T10:15");

        assertRefused(() -> withoutExpert(0).book(EXPERT_2.id(), zurich("2026-11-17T10:00"),
                SEVENTEENTH_NINE_TO_TWELVE, now), CaseForBooking.SLOT_NOT_AVAILABLE, SLOT_NOT_AVAILABLE);
        assertThat(withoutExpert(0).book(EXPERT_2.id(), zurich("2026-11-17T10:30"), SEVENTEENTH_NINE_TO_TWELVE, now))
                .isNotNull();
    }

    @Test
    void theBookedAppointmentsAreNotNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> withoutExpert(-1))
                .withMessage("A case has no negative number of booked appointments: -1");
    }
}
