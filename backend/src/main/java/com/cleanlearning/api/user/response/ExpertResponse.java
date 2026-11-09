package com.cleanlearning.api.user.response;

import java.util.UUID;

import com.cleanlearning.domain.user.Expert;

/**
 * An expert the admin can make eligible for a consultation offering.
 */
public record ExpertResponse(UUID id, String displayName) {

    public static ExpertResponse from(Expert expert) {
        return new ExpertResponse(expert.id().value(), expert.displayName().value());
    }
}
