package com.cleanlearning.application.availability;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.availability.port.LoadAvailabilityPort;
import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.user.CurrentUser;

/**
 * UC-13 step 2: the expert opens their availability. It's always the current user's own.
 */
@Service
public class GetAvailabilityUseCase {

    private final LoadAvailabilityPort loadAvailability;

    private final Clock clock;

    GetAvailabilityUseCase(LoadAvailabilityPort loadAvailability, Clock clock) {
        this.loadAvailability = loadAvailability;
        this.clock = clock;
    }

    /** The current user's dates from today (Zurich) on, and their time ranges; both empty without a saved one. */
    @Transactional(readOnly = true)
    public Availability invoke(CurrentUser user) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), BusinessTimeZone.ZONE);
        return loadAvailability.load(user.id(), today);
    }
}
