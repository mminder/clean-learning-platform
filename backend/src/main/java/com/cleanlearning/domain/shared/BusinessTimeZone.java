package com.cleanlearning.domain.shared;

import java.time.ZoneId;

/** Every local date and time of the platform is one in Zurich; instants don't depend on it. */
public final class BusinessTimeZone {

  public static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

  private BusinessTimeZone() {}
}
