package com.cleanlearning.domain.consultation.value;

import java.time.Instant;
import java.util.Objects;

/**
 * A time a customer can book on their case (UC-06 steps 3–4): the session from start to end, and until when it can be
 * cancelled for free, which the confirmation states (UC-06 step 5). It shows nothing of the expert's other
 * appointments.
 */
public record BookableSlot(Instant start, Instant end, Instant freeCancellationUntil) {

    public BookableSlot {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(freeCancellationUntil, "freeCancellationUntil");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("A bookable slot ends after it starts: " + start + " to " + end + ".");
        }
        if (freeCancellationUntil.isAfter(start)) {
            throw new IllegalArgumentException(
                    "Free cancellation ends by the start: " + freeCancellationUntil + " after " + start + ".");
        }
    }
}
