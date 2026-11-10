package com.cleanlearning.domain.consultation;

import java.util.Objects;

import com.cleanlearning.domain.user.value.UserId;

/**
 * What a booking writes (UC-06 step 7): the new appointment, and on the first booking the expert it assigns to the
 * case.
 *
 * @param expertId the appointment's expert: the case's, or the one the first booking assigns
 * @param assignsExpert whether the case had no expert, so the booking assigns {@code expertId} to it
 */
public record Booking(Appointment appointment, UserId expertId, boolean assignsExpert) {

    public Booking {
        Objects.requireNonNull(appointment, "appointment");
        Objects.requireNonNull(expertId, "expertId");
    }
}
