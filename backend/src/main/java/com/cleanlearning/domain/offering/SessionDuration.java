package com.cleanlearning.domain.offering;

/**
 * How long one session of a consultation offering takes, in minutes.
 */
public record SessionDuration(int minutes) {

    public SessionDuration {
        if (minutes <= 0) {
            throw new IllegalArgumentException("A session lasts at least one minute: " + minutes);
        }
    }
}
