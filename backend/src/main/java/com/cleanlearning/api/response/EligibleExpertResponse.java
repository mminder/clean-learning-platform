package com.cleanlearning.api.response;

import java.util.UUID;

import com.cleanlearning.domain.offering.EligibleExpert;

/**
 * An eligible expert with the public fields of their profile. The shop's offering detail and the booking's expert
 * picker answer it (UC-02, UC-06).
 */
public record EligibleExpertResponse(UUID id, String displayName, String bio) {

    public static EligibleExpertResponse from(EligibleExpert expert) {
        return new EligibleExpertResponse(expert.id().value(), expert.displayName().value(), expert.bio());
    }
}
