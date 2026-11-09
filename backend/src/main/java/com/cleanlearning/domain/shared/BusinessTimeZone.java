package com.cleanlearning.domain.shared;

import java.time.ZoneId;

/**
 * The business time zone, Europe/Zurich: every local date and time of the platform is one in Zurich, such as today,
 * an availability's dates and time ranges, and the slot grid (D-09, D-10). Instants don't depend on it.
 */
public final class BusinessTimeZone {

    public static final ZoneId ZONE = ZoneId.of("Europe/Zurich");

    private BusinessTimeZone() {
    }
}
