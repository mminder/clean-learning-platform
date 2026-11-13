package com.cleanlearning.domain.consultation;

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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The bookable slots of one expert (D-10, UC-06): the start times on the 30-minute grid, in Zurich
 * wall-clock time, where a whole session fits inside one availability range, overlaps no busy time,
 * and starts at least 24 hours from now. The grid and the ranges are wall-clock times, but a
 * session lasts its real duration.
 *
 * @param availability the expert's dates and time ranges
 * @param busyTimes the expert's other commitments and the customer's own appointments, in any order
 */
public record SlotFinder(Availability availability, List<BusyTime> busyTimes) {

  public static final Duration GRID = Duration.ofMinutes(30);

  public static final Duration MINIMUM_NOTICE = Duration.ofHours(24);

  public SlotFinder {
    Objects.requireNonNull(availability, "availability");
    busyTimes = List.copyOf(busyTimes);
  }

  /** Every start time at which a session of this duration can be booked, soonest first. */
  public List<Instant> startTimes(SessionDuration duration, Instant now) {
    Duration session = Duration.ofMinutes(duration.minutes());
    Instant earliest = now.plus(MINIMUM_NOTICE);
    List<Instant> startTimes = new ArrayList<>();
    for (LocalDate date : availability.dates()) {
      for (TimeRange range : availability.timeRanges()) {
        Instant rangeEnd = date.atTime(range.end()).atZone(BusinessTimeZone.ZONE).toInstant();
        for (LocalTime time : gridTimes(range)) {
          Instant start = instantOf(date.atTime(time));
          if (start == null || start.isBefore(earliest)) {
            continue;
          }
          Instant end = start.plus(session);
          if (!end.isAfter(rangeEnd) && !isBusy(start, end)) {
            startTimes.add(start);
          }
        }
      }
    }
    return startTimes;
  }

  /**
   * Whether a session of this duration can be booked at exactly this start: one of the start times,
   * no other.
   */
  public boolean isBookable(Instant start, SessionDuration duration, Instant now) {
    return startTimes(duration, now).contains(start);
  }

  /**
   * Whether the session from start to end overlaps busy time; touching it is fine, as there's no
   * buffer.
   */
  private boolean isBusy(Instant start, Instant end) {
    return busyTimes.stream()
        .anyMatch(busy -> busy.start().isBefore(end) && start.isBefore(busy.end()));
  }

  /**
   * The wall-clock times on the grid from the range's start, rounded up to the grid, to before its
   * end.
   */
  private static List<LocalTime> gridTimes(TimeRange range) {
    long grid = GRID.toNanos();
    long first = Math.ceilDiv(range.start().toNanoOfDay(), grid) * grid;
    List<LocalTime> times = new ArrayList<>();
    // The end is a time of the day, so the loop ends before midnight and the nanos never wrap.
    for (long nanos = first; nanos < range.end().toNanoOfDay(); nanos += grid) {
      times.add(LocalTime.ofNanoOfDay(nanos));
    }
    return times;
  }

  /**
   * The instant of a wall-clock time that exists exactly once, otherwise null: spring's gap,
   * autumn's overlap.
   */
  private static Instant instantOf(LocalDateTime dateTime) {
    List<ZoneOffset> offsets = BusinessTimeZone.ZONE.getRules().getValidOffsets(dateTime);
    return offsets.size() == 1 ? dateTime.toInstant(offsets.get(0)) : null;
  }
}
