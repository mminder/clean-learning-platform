package com.cleanlearning.application.user.port;

import java.util.List;

import com.cleanlearning.domain.user.User;

public interface LoadUsersPort {

    /**
     * Every user with their roles, sorted by name.
     */
    List<User> loadAll();
}
