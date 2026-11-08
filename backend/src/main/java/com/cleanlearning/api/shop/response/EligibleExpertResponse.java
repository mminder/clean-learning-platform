package com.cleanlearning.api.shop.response;

import java.util.UUID;

import com.cleanlearning.domain.offering.EligibleExpert;

/**
 * An eligible expert with the public fields of their profile.
 */
public record EligibleExpertResponse(UUID id, String displayName, String bio) {

    public static EligibleExpertResponse from(EligibleExpert expert) {
        return new EligibleExpertResponse(expert.id().value(), expert.displayName().value(), expert.bio());
    }
}
