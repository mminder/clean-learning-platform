package com.cleanlearning.application.user.port;

import java.util.Optional;

import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.UserId;

public interface LoadUserPort {

    /**
     * The user with their roles.
     */
    Optional<User> load(UserId id);
}
