package com.cleanlearning.domain.offering.value;

/**
 * How many sessions a package of a consultation offering holds. It is at least one.
 */
public record SessionCount(int value) {

    public SessionCount {
        if (value <= 0) {
            throw new IllegalArgumentException("A package has at least one session: " + value);
        }
    }
}
