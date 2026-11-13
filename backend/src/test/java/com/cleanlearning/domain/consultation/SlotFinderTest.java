package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.availability.value.TimeRange;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * The bookable slot of D-10 and UC-06, as spec 04's "bookable slots" scenarios describe it. The
 * spec's "now" is Mon 9 Nov 2026, 15:00 in Zurich. Instants are written with their Zurich time, and
 * start times are compared as Zurich times, so the tests read like the scenarios.
 */
class SlotFinderTest {

  private static final DateTimeFormatter LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static final Instant NOW = zurich("2026-11-09 15:00");

  private static final SessionDuration THIRTY = new SessionDuration(30);
  private static final SessionDuration SIXTY = new SessionDuration(60);
  private static final SessionDuration NINETY = new SessionDuration(90);

  private static Instant zurich(String dateAndTime) {
    return LocalDateTime.parse(dateAndTime, LOCAL).atZone(BusinessTimeZone.ZONE).toInstant();
  }

  private static TimeRange range(String start, String end) {
    return new TimeRange(LocalTime.parse(start), LocalTime.parse(end));
  }

  private static Availability availability(List<String> dates, TimeRange... ranges) {
    SortedSet<LocalDate> sorted = new TreeSet<>();
    dates.forEach(date -> sorted.add(LocalDate.parse(date)));
    return new Availability(sorted, List.of(ranges));
  }

  private static Availability availability(String date, TimeRange... ranges) {
    return availability(List.of(date), ranges);
  }

  private static BusyTime busy(String start, String end) {
    return new BusyTime(zurich(start), zurich(end));
  }

  private static List<String> startTimes(
      Availability availability, List<BusyTime> busyTimes, SessionDuration duration, Instant now) {
    return new SlotFinder(availability, busyTimes)
        .startTimes(duration, now).stream()
            .map(start -> LocalDateTime.ofInstant(start, BusinessTimeZone.ZONE).format(LOCAL))
            .toList();
  }

  private static List<String> startTimes(Availability availability, SessionDuration duration) {
    return startTimes(availability, List.of(), duration, NOW);
  }

  @Test
  void offersEveryHalfHourWhereASessionFitsInTheRange() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly(
            "2026-11-17 09:00",
            "2026-11-17 09:30",
            "2026-11-17 10:00",
            "2026-11-17 10:30",
            "2026-11-17 11:00");
    assertThat(startTimes(availability, THIRTY))
        .containsExactly(
            "2026-11-17 09:00",
            "2026-11-17 09:30",
            "2026-11-17 10:00",
            "2026-11-17 10:30",
            "2026-11-17 11:00",
            "2026-11-17 11:30");
    assertThat(startTimes(availability, NINETY))
        .containsExactly(
            "2026-11-17 09:00", "2026-11-17 09:30", "2026-11-17 10:00", "2026-11-17 10:30");
  }

  @Test
  void roundsARangeThatIsOffTheGridInToTheGrid() {
    Availability availability = availability("2026-11-17", range("09:15", "11:45"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly("2026-11-17 09:30", "2026-11-17 10:00", "2026-11-17 10:30");
    assertThat(startTimes(availability, THIRTY))
        .containsExactly(
            "2026-11-17 09:30", "2026-11-17 10:00", "2026-11-17 10:30", "2026-11-17 11:00");
  }

  @Test
  void offersASessionThatEndsExactlyWhenTheRangeEnds() {
    assertThat(startTimes(availability("2026-11-17", range("09:00", "10:00")), SIXTY))
        .containsExactly("2026-11-17 09:00");
    assertThat(startTimes(availability("2026-11-17", range("09:00", "09:59")), SIXTY)).isEmpty();
  }

  @Test
  void offersNothingWhenTheRangeIsShorterThanASession() {
    assertThat(startTimes(availability("2026-11-17", range("09:00", "10:00")), NINETY)).isEmpty();
  }

  @Test
  void offersNothingWhenTheRangeHasNoGridTime() {
    assertThat(startTimes(availability("2026-11-17", range("09:05", "09:25")), THIRTY)).isEmpty();
  }

  @Test
  void doesNotOfferASessionThatSpansTheBreakBetweenTwoRanges() {
    Availability availability =
        availability("2026-11-17", range("09:00", "12:00"), range("13:00", "17:00"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly(
            "2026-11-17 09:00",
            "2026-11-17 09:30",
            "2026-11-17 10:00",
            "2026-11-17 10:30",
            "2026-11-17 11:00",
            "2026-11-17 13:00",
            "2026-11-17 13:30",
            "2026-11-17 14:00",
            "2026-11-17 14:30",
            "2026-11-17 15:00",
            "2026-11-17 15:30",
            "2026-11-17 16:00");
    assertThat(startTimes(availability, SIXTY))
        .doesNotContain("2026-11-17 11:30", "2026-11-17 12:00", "2026-11-17 12:30");
  }

  @Test
  void listsSeveralDatesInOrder() {
    Availability availability =
        availability(List.of("2026-11-18", "2026-11-17"), range("09:00", "10:00"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly("2026-11-17 09:00", "2026-11-18 09:00");
  }

  @Test
  void offersNothingWithoutDatesOrRanges() {
    assertThat(startTimes(availability(List.of(), range("09:00", "12:00")), SIXTY)).isEmpty();
    assertThat(startTimes(availability("2026-11-17"), SIXTY)).isEmpty();
    assertThat(startTimes(availability(List.of()), SIXTY)).isEmpty();
  }

  @Test
  void requiresTwentyFourHoursNotice() {
    Instant now = zurich("2026-11-16 10:15");
    Availability availability =
        availability(List.of("2026-11-16", "2026-11-17"), range("09:00", "12:00"));

    assertThat(startTimes(availability, List.of(), SIXTY, now))
        .containsExactly("2026-11-17 10:30", "2026-11-17 11:00");
  }

  @Test
  void offersAStartExactlyTwentyFourHoursFromNow() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));

    assertThat(startTimes(availability, List.of(), SIXTY, zurich("2026-11-16 10:00")))
        .containsExactly("2026-11-17 10:00", "2026-11-17 10:30", "2026-11-17 11:00");
  }

  @Test
  void doesNotOfferAStartOneMinuteShortOfTwentyFourHours() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));

    assertThat(startTimes(availability, List.of(), SIXTY, zurich("2026-11-16 10:01")))
        .containsExactly("2026-11-17 10:30", "2026-11-17 11:00");
  }

  @Test
  void measuresTheNoticeInRealTime() {
    // 24 hours before Sun 28 Mar 2027, 12:00 (CEST) is Sat 27 Mar, 11:00 (CET): the clocks jump in
    // between.
    Availability availability = availability("2027-03-28", range("12:00", "13:00"));

    assertThat(startTimes(availability, List.of(), THIRTY, zurich("2027-03-27 11:00")))
        .containsExactly("2027-03-28 12:00", "2027-03-28 12:30");
    assertThat(startTimes(availability, List.of(), THIRTY, zurich("2027-03-27 11:01")))
        .containsExactly("2027-03-28 12:30");
  }

  @Test
  void doesNotOfferASessionThatOverlapsAnAppointment() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));
    List<BusyTime> busyTimes = List.of(busy("2026-11-17 10:00", "2026-11-17 11:00"));

    assertThat(startTimes(availability, busyTimes, SIXTY, NOW))
        .containsExactly("2026-11-17 09:00", "2026-11-17 11:00");
  }

  @Test
  void offersASessionThatTouchesBusyTimeAtEitherEnd() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));
    List<BusyTime> busyTimes = List.of(busy("2026-11-17 10:00", "2026-11-17 11:00"));

    assertThat(startTimes(availability, busyTimes, SIXTY, NOW))
        .contains("2026-11-17 09:00", "2026-11-17 11:00")
        .doesNotContain("2026-11-17 09:30", "2026-11-17 10:00", "2026-11-17 10:30");
    assertThat(startTimes(availability, busyTimes, THIRTY, NOW))
        .contains("2026-11-17 09:30", "2026-11-17 11:00")
        .doesNotContain("2026-11-17 10:00", "2026-11-17 10:30");
  }

  @Test
  void doesNotOfferASessionInsideLongerBusyTime() {
    Availability availability = availability("2026-11-17", range("09:00", "17:00"));
    List<BusyTime> busyTimes = List.of(busy("2026-11-17 09:00", "2026-11-17 15:00"));

    assertThat(startTimes(availability, busyTimes, THIRTY, NOW))
        .containsExactly(
            "2026-11-17 15:00", "2026-11-17 15:30", "2026-11-17 16:00", "2026-11-17 16:30");
  }

  @Test
  void doesNotOfferASessionThatContainsBusyTime() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));
    List<BusyTime> busyTimes = List.of(busy("2026-11-17 10:05", "2026-11-17 10:25"));

    assertThat(startTimes(availability, busyTimes, NINETY, NOW))
        .containsExactly("2026-11-17 10:30");
    assertThat(startTimes(availability, busyTimes, THIRTY, NOW)).doesNotContain("2026-11-17 10:00");
  }

  @Test
  void ignoresBusyTimeOnAnotherDate() {
    Availability availability = availability("2026-11-17", range("09:00", "10:00"));
    List<BusyTime> busyTimes =
        List.of(
            busy("2026-11-18 09:00", "2026-11-18 10:00"),
            busy("2026-11-16 09:00", "2026-11-16 10:00"));

    assertThat(startTimes(availability, busyTimes, SIXTY, NOW)).containsExactly("2026-11-17 09:00");
  }

  @Test
  void takesSeveralBusyTimesInAnyOrder() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));
    List<BusyTime> busyTimes =
        List.of(
            busy("2026-11-17 11:00", "2026-11-17 11:30"),
            busy("2026-11-17 09:00", "2026-11-17 09:30"));

    assertThat(startTimes(availability, busyTimes, THIRTY, NOW))
        .containsExactly(
            "2026-11-17 09:30", "2026-11-17 10:00", "2026-11-17 10:30", "2026-11-17 11:30");
  }

  @Test
  void offersNothingWhenEverySlotIsTaken() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));
    List<BusyTime> busyTimes = List.of(busy("2026-11-17 09:00", "2026-11-17 12:00"));

    assertThat(startTimes(availability, busyTimes, SIXTY, NOW)).isEmpty();
  }

  // The days of the summer-time changes (D-10, spec decision 11): the grid and the ranges are
  // wall-clock times, a
  // session lasts its real duration.

  @Test
  void offersNoStartTimeInTheHourThatDoesNotExistInSpring() {
    // Sun 28 Mar 2027: the clocks jump from 02:00 to 03:00.
    Availability availability = availability("2027-03-28", range("01:00", "05:00"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly(
            "2027-03-28 01:00",
            "2027-03-28 01:30",
            "2027-03-28 03:00",
            "2027-03-28 03:30",
            "2027-03-28 04:00");
    assertThat(startTimes(availability, THIRTY))
        .doesNotContain("2027-03-28 02:00", "2027-03-28 02:30");
  }

  @Test
  void lastsTheRealDurationAcrossSpringsChange() {
    SlotFinder finder =
        new SlotFinder(availability("2027-03-28", range("01:00", "05:00")), List.of());
    Instant start = zurich("2027-03-28 01:30");

    // 01:30 is 01:30–03:30 on the clocks, and 60 real minutes.
    assertThat(finder.startTimes(SIXTY, NOW)).contains(start);
    assertThat(start.plus(Duration.ofMinutes(60))).isEqualTo(zurich("2027-03-28 03:30"));
  }

  @Test
  void endsASessionBeforeTheRangeEndsInRealTimeInSpring() {
    // 00:00–03:00 is two real hours on 28 Mar 2027, so 01:30–02:30 on the clocks is 01:30–03:30:
    // too long.
    Availability availability = availability("2027-03-28", range("00:00", "03:00"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly("2027-03-28 00:00", "2027-03-28 00:30", "2027-03-28 01:00");
    assertThat(startTimes(availability, NINETY))
        .containsExactly("2027-03-28 00:00", "2027-03-28 00:30");
  }

  @Test
  void endsARangeThatEndsInSpringsGapAtTheInstantJavaGivesIt() {
    // 02:30 doesn't exist on 28 Mar 2027; Java moves it to 03:30, the instant 01:30 UTC.
    Availability availability = availability("2027-03-28", range("01:00", "02:30"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly("2027-03-28 01:00", "2027-03-28 01:30");
    assertThat(startTimes(availability, NINETY)).containsExactly("2027-03-28 01:00");
  }

  @Test
  void offersNoStartTimeInTheHourThatHappensTwiceInAutumn() {
    // Sun 31 Oct 2027: the clocks go back from 03:00 to 02:00, so 02:00–02:59 happens twice.
    Availability availability = availability("2027-10-31", range("01:00", "05:00"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly(
            "2027-10-31 01:00",
            "2027-10-31 01:30",
            "2027-10-31 03:00",
            "2027-10-31 03:30",
            "2027-10-31 04:00");
    assertThat(startTimes(availability, THIRTY))
        .doesNotContain("2027-10-31 02:00", "2027-10-31 02:30");
  }

  @Test
  void lastsTheRealDurationAcrossAutumnsChange() {
    // 01:00–03:00 is three real hours on 31 Oct 2027. A session from 01:30 (CEST) lasts until 02:30
    // (CET).
    Availability availability = availability("2027-10-31", range("01:00", "03:00"));

    assertThat(startTimes(availability, new SessionDuration(120)))
        .containsExactly("2027-10-31 01:00", "2027-10-31 01:30");
    assertThat(startTimes(availability, new SessionDuration(150)))
        .containsExactly("2027-10-31 01:00", "2027-10-31 01:30");
    assertThat(startTimes(availability, new SessionDuration(180)))
        .containsExactly("2027-10-31 01:00");
  }

  @Test
  void endsARangeThatEndsInAutumnsOverlapAtTheEarlierInstant() {
    // 02:30 happens twice on 31 Oct 2027; Java takes the first, the instant 00:30 UTC.
    Availability availability = availability("2027-10-31", range("01:00", "02:30"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly("2027-10-31 01:00", "2027-10-31 01:30");
    assertThat(startTimes(availability, NINETY)).containsExactly("2027-10-31 01:00");
  }

  @Test
  void keepsBusyTimeInRealTimeAcrossAutumnsChange() {
    Availability availability = availability("2027-10-31", range("01:00", "05:00"));
    // 03:00–04:00 CET, which is 02:00–03:00 UTC.
    List<BusyTime> busyTimes =
        List.of(
            new BusyTime(
                Instant.parse("2027-10-31T02:00:00Z"), Instant.parse("2027-10-31T03:00:00Z")));

    assertThat(startTimes(availability, busyTimes, SIXTY, NOW))
        .containsExactly("2027-10-31 01:00", "2027-10-31 01:30", "2027-10-31 04:00");
  }

  @Test
  void behavesAsOnAnyOtherDayJustAfterTheChange() {
    Availability availability =
        availability(List.of("2027-03-29", "2027-11-01"), range("09:00", "10:00"));

    assertThat(startTimes(availability, SIXTY))
        .containsExactly("2027-03-29 09:00", "2027-11-01 09:00");
  }

  // isBookable is startTimes, as one question about one start.

  @Test
  void isBookableForEveryStartTime() {
    Availability availability =
        availability(
            List.of("2026-11-17", "2027-03-28", "2027-10-31"),
            range("01:00", "05:00"),
            range("09:00", "12:00"));
    SlotFinder finder =
        new SlotFinder(availability, List.of(busy("2026-11-17 09:30", "2026-11-17 10:00")));

    List<Instant> startTimes = finder.startTimes(SIXTY, NOW);

    assertThat(startTimes).isNotEmpty();
    assertThat(startTimes)
        .allSatisfy(start -> assertThat(finder.isBookable(start, SIXTY, NOW)).isTrue());
  }

  @Test
  void isNotBookableForAStartThatIsNotOffered() {
    Availability availability = availability("2026-11-17", range("09:00", "12:00"));
    SlotFinder finder =
        new SlotFinder(availability, List.of(busy("2026-11-17 10:00", "2026-11-17 11:00")));

    assertThat(finder.isBookable(zurich("2026-11-17 10:00"), SIXTY, NOW)).isFalse(); // busy
    assertThat(finder.isBookable(zurich("2026-11-17 11:30"), SIXTY, NOW)).isFalse(); // doesn't fit
    assertThat(finder.isBookable(zurich("2026-11-17 08:30"), SIXTY, NOW))
        .isFalse(); // before the range
    assertThat(finder.isBookable(zurich("2026-11-18 09:00"), SIXTY, NOW)).isFalse(); // another date
    assertThat(finder.isBookable(zurich("2026-11-17 09:00"), SIXTY, zurich("2026-11-16 09:01")))
        .isFalse();
  }

  @Test
  void isNotBookableForAStartOffTheGrid() {
    SlotFinder finder =
        new SlotFinder(availability("2026-11-17", range("09:00", "12:00")), List.of());
    Instant offered = zurich("2026-11-17 10:00");

    assertThat(finder.isBookable(offered, SIXTY, NOW)).isTrue();
    assertThat(finder.isBookable(offered.plusSeconds(60), SIXTY, NOW)).isFalse();
    assertThat(finder.isBookable(offered.minusSeconds(60), SIXTY, NOW)).isFalse();
    assertThat(finder.isBookable(offered.plusSeconds(1), SIXTY, NOW)).isFalse();
    assertThat(finder.isBookable(offered.plusSeconds(15 * 60), SIXTY, NOW)).isFalse();
  }

  @Test
  void isNotBookableInTheHoursThatDoNotExistOrHappenTwice() {
    SlotFinder spring =
        new SlotFinder(availability("2027-03-28", range("01:00", "05:00")), List.of());
    SlotFinder autumn =
        new SlotFinder(availability("2027-10-31", range("01:00", "05:00")), List.of());

    // Spring: 02:00 doesn't exist; 03:00 CEST is 01:00 UTC. Autumn: 02:00 CEST and 02:00 CET are
    // both skipped.
    assertThat(spring.isBookable(Instant.parse("2027-03-28T01:00:00Z"), SIXTY, NOW)).isTrue();
    assertThat(autumn.isBookable(Instant.parse("2027-10-31T00:00:00Z"), SIXTY, NOW)).isFalse();
    assertThat(autumn.isBookable(Instant.parse("2027-10-31T01:00:00Z"), SIXTY, NOW)).isFalse();
    assertThat(autumn.isBookable(Instant.parse("2027-10-31T02:00:00Z"), SIXTY, NOW)).isTrue();
  }

  @Test
  void needsAvailabilityAndABusyTimeList() {
    assertThatNullPointerException()
        .isThrownBy(() -> new SlotFinder(null, List.of()))
        .withMessage("availability");
  }
}
