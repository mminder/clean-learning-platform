package com.cleanlearning.domain.offering;

import java.util.Objects;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;

/**
 * An expert a consultation offering's customers can pick, with the public fields of their expert profile.
 */
public record EligibleExpert(UserId id, DisplayName displayName, String bio) {

    public EligibleExpert {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(bio, "bio");
    }
}
