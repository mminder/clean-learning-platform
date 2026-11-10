package com.cleanlearning.application.availability.port;

import java.time.LocalDate;

import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.user.value.UserId;

public interface ReplaceAvailabilityPort {

    /**
     * Replaces the expert's dates from {@code from} on, and all their time ranges, with the availability's. Their
     * dates before {@code from} stay as they are.
     */
    void replaceFrom(UserId expertId, LocalDate from, Availability availability);
}
