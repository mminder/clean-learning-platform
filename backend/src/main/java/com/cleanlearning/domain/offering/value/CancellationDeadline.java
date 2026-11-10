package com.cleanlearning.domain.offering.value;

import java.time.Duration;
import java.time.Instant;

/**
 * How many hours before a session it can still be cancelled for free. Zero means until the session starts.
 */
public record CancellationDeadline(int hours) {

    public CancellationDeadline {
        if (hours < 0) {
            throw new IllegalArgumentException("A cancellation deadline is not negative: " + hours);
        }
    }

    /**
     * Until when a session starting at {@code start} can be cancelled for free: {@code start − deadline}, in real
     * hours (D-11). Cancelling before it is free (UC-07); the booking's confirmation states it (UC-06 step 5).
     */
    public Instant freeUntil(Instant start) {
        return start.minus(Duration.ofHours(hours));
    }
}
