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

/** The grid and the ranges are wall-clock times, but a session lasts its real duration. */
public record SlotFinder(Availability availability, List<BusyTime> busyTimes) {

  public static final Duration GRID = Duration.ofMinutes(30);

  public static final Duration MINIMUM_NOTICE = Duration.ofHours(24);

  public SlotFinder {
    Objects.requireNonNull(availability, "availability");
    busyTimes = List.copyOf(busyTimes);
  }

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

  public boolean isBookable(Instant start, SessionDuration duration, Instant now) {
    return startTimes(duration, now).contains(start);
  }

  /** Touching busy time is fine: there's no buffer. */
  private boolean isBusy(Instant start, Instant end) {
    return busyTimes.stream()
        .anyMatch(busy -> busy.start().isBefore(end) && start.isBefore(busy.end()));
  }

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

  /** Null for a time that doesn't exist exactly once: spring's gap, autumn's overlap. */
  private static Instant instantOf(LocalDateTime dateTime) {
    List<ZoneOffset> offsets = BusinessTimeZone.ZONE.getRules().getValidOffsets(dateTime);
    return offsets.size() == 1 ? dateTime.toInstant(offsets.get(0)) : null;
  }
}
