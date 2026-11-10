package com.cleanlearning.application.availability.port;

import java.time.LocalDate;

import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.user.value.UserId;

public interface LoadAvailabilityPort {

    /**
     * The expert's availability: their dates from {@code from} on, and all their time ranges. Without a saved
     * availability, both are empty.
     */
    Availability load(UserId expertId, LocalDate from);
}
