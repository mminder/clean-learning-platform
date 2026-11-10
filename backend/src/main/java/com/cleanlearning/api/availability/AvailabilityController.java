package com.cleanlearning.api.availability;

import java.util.HashSet;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.availability.request.AvailabilityRequest;
import com.cleanlearning.api.availability.response.AvailabilityResponse;
import com.cleanlearning.application.availability.GetAvailabilityUseCase;
import com.cleanlearning.application.availability.SaveAvailabilityCommand;
import com.cleanlearning.application.availability.SaveAvailabilityUseCase;
import com.cleanlearning.domain.availability.value.TimeRange;
import com.cleanlearning.domain.user.CurrentUser;

import jakarta.validation.Valid;

/**
 * The expert's own availability (UC-13). Security lets only users with {@code EXPERT} in; the route has no id, as the
 * availability is always the current user's.
 */
@RestController
@RequestMapping(AvailabilityController.PATH)
@PreAuthorize("hasRole('EXPERT')")
class AvailabilityController {

    static final String PATH = "/api/expert/availability";

    private final GetAvailabilityUseCase getAvailability;

    private final SaveAvailabilityUseCase saveAvailability;

    AvailabilityController(GetAvailabilityUseCase getAvailability, SaveAvailabilityUseCase saveAvailability) {
        this.getAvailability = getAvailability;
        this.saveAvailability = saveAvailability;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    AvailabilityResponse getAvailability(@AuthenticationPrincipal CurrentUser currentUser) {
        return AvailabilityResponse.from(getAvailability.invoke(currentUser));
    }

    /** Replaces the dates from today on and the time ranges, and answers the saved availability. */
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    @ProblemResponses.Status400
    @ProblemResponses.Status415
    AvailabilityResponse saveAvailability(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody AvailabilityRequest request) {
        return AvailabilityResponse.from(saveAvailability.invoke(currentUser, toCommand(request)));
    }

    /** A value object that rejects a value Bean Validation let through throws, and the API answers 400. */
    private static SaveAvailabilityCommand toCommand(AvailabilityRequest request) {
        return new SaveAvailabilityCommand(new HashSet<>(request.dates()),
                request.timeRanges().stream().map(range -> new TimeRange(range.start(), range.end())).toList());
    }
}
