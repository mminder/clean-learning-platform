package com.cleanlearning.domain.shared;

import java.time.Duration;
import java.time.Instant;

/**
 * When a call's link is shown to a customer (UC-10, D-19): from one hour before the start until the
 * end, so it includes 09:00 and excludes 11:00 for a call 10:00–11:00. Experts always see their
 * links. The consultation appointments use it, and the training days will.
 */
public final class LinkWindow {

  public static final Duration OPENS_BEFORE_START = Duration.ofHours(1);

  private LinkWindow() {}

  public static Instant opensAt(Instant start) {
    return start.minus(OPENS_BEFORE_START);
  }

  public static boolean isOpenAt(Instant start, Instant end, Instant asOf) {
    return !asOf.isBefore(opensAt(start)) && asOf.isBefore(end);
  }
}
