package com.cleanlearning.domain.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.availability.value.TimeRange;

/**
 * UC-13's rules, as spec 04's decision 9 adds them: the ranges neither overlap nor touch, and a date before today
 * can't be changed, while today can.
 */
class AvailabilityTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    private static final TimeRange MORNING = range("09:00", "12:00");

    private static final TimeRange AFTERNOON = range("13:00", "17:00");

    private static TimeRange range(String start, String end) {
        return new TimeRange(LocalTime.parse(start), LocalTime.parse(end));
    }

    private static SortedSet<LocalDate> dates(String... dates) {
        SortedSet<LocalDate> result = new TreeSet<>();
        for (String date : dates) {
            result.add(LocalDate.parse(date));
        }
        return result;
    }

    @Test
    void holdsTheDatesAndTheRanges() {
        Availability availability = new Availability(dates("2026-11-18", "2026-11-17"), List.of(MORNING, AFTERNOON));

        assertThat(availability.dates()).containsExactly(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18));
        assertThat(availability.timeRanges()).containsExactly(MORNING, AFTERNOON);
    }

    @Test
    void sortsTheRangesByStart() {
        assertThat(new Availability(dates("2026-11-17"), List.of(AFTERNOON, MORNING)).timeRanges())
                .containsExactly(MORNING, AFTERNOON);
    }

    @Test
    void mayHaveNoDatesAndNoRanges() {
        Availability none = new Availability(dates(), List.of());

        assertThat(none.dates()).isEmpty();
        assertThat(none.timeRanges()).isEmpty();
    }

    @Test
    void copiesItsInputs() {
        SortedSet<LocalDate> dates = dates("2026-11-17");
        List<TimeRange> ranges = new ArrayList<>(List.of(MORNING));
        Availability availability = new Availability(dates, ranges);

        dates.add(LocalDate.of(2026, 11, 18));
        ranges.add(AFTERNOON);

        assertThat(availability.dates()).containsExactly(LocalDate.of(2026, 11, 17));
        assertThat(availability.timeRanges()).containsExactly(MORNING);
    }

    @Test
    void rejectsOverlappingRanges() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Availability(dates("2026-11-17"), List.of(MORNING, range("11:00", "14:00"))))
                .withMessage("Time ranges must not overlap or touch: 09:00–12:00 and 11:00–14:00.");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Availability(dates("2026-11-17"), List.of(MORNING, range("10:00", "11:00"))));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Availability(dates("2026-11-17"), List.of(MORNING, MORNING)));
    }

    @Test
    void rejectsTouchingRanges() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Availability(dates("2026-11-17"), List.of(MORNING, range("12:00", "15:00"))))
                .withMessage("Time ranges must not overlap or touch: 09:00–12:00 and 12:00–15:00.");
    }

    @Test
    void findsAnOverlapWhateverTheOrder() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Availability(dates("2026-11-17"),
                List.of(range("16:00", "18:00"), MORNING, AFTERNOON)))
                .withMessage("Time ranges must not overlap or touch: 13:00–17:00 and 16:00–18:00.");
    }

    @Test
    void acceptsRangesWithAGapOfAMinute() {
        assertThat(new Availability(dates("2026-11-17"), List.of(MORNING, range("12:01", "13:00"))).timeRanges())
                .hasSize(2);
    }

    @Test
    void allowsTodayAndLaterDates() {
        Availability availability = new Availability(dates("2026-11-09", "2026-11-10"), List.of(MORNING));

        assertThatNoException().isThrownBy(() -> availability.requireNoDateBefore(TODAY));
    }

    @Test
    void rejectsADateBeforeToday() {
        Availability availability = new Availability(dates("2026-11-07", "2026-11-08", "2026-11-17"), List.of());

        assertThatIllegalArgumentException().isThrownBy(() -> availability.requireNoDateBefore(TODAY))
                .withMessage("A date before today can't be changed: 2026-11-07.");
    }

    @Test
    void allowsNoDatesAtAll() {
        assertThatNoException().isThrownBy(() -> new Availability(dates(), List.of()).requireNoDateBefore(TODAY));
    }
}
