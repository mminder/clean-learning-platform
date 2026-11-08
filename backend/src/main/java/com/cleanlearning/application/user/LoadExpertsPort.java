package com.cleanlearning.application.user;

import java.util.List;

import com.cleanlearning.domain.user.Expert;

public interface LoadExpertsPort {

    /**
     * Every user with {@code EXPERT} and an expert profile, sorted by display name, then id.
     */
    List<Expert> loadExperts();
}
