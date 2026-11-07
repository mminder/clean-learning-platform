package com.cleanlearning.domain.offering;

/**
 * How many hours before a session it can still be cancelled for free. Zero means until the session starts.
 */
public record CancellationDeadline(int hours) {

    public CancellationDeadline {
        if (hours < 0) {
            throw new IllegalArgumentException("A cancellation deadline is not negative: " + hours);
        }
    }
}
