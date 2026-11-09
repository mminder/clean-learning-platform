package com.cleanlearning.application.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.user.port.LoadUserPort;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.User;

/**
 * UC-01: who is logged in, with name, email and roles read fresh from the database.
 */
@Service
public class GetCurrentUserUseCase {

    private final LoadUserPort loadUser;

    GetCurrentUserUseCase(LoadUserPort loadUser) {
        this.loadUser = loadUser;
    }

    /**
     * @throws NotFoundException if the user no longer exists
     */
    @Transactional(readOnly = true)
    public User invoke(CurrentUser currentUser) {
        return loadUser.load(currentUser.id())
                .orElseThrow(() -> new NotFoundException("User " + currentUser.id().value() + " not found."));
    }
}
