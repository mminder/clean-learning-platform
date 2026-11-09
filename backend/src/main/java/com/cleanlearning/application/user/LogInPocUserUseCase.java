package com.cleanlearning.application.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.user.port.LoadUserPort;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.UserId;

/**
 * UC-01 PoC steps 3–4: the picked user, with the roles they log in with. The controller starts the session.
 */
@Service
public class LogInPocUserUseCase {

    private final LoadUserPort loadUser;

    LogInPocUserUseCase(LoadUserPort loadUser) {
        this.loadUser = loadUser;
    }

    /**
     * @throws NotFoundException if the user doesn't exist
     */
    @Transactional(readOnly = true)
    public User invoke(UserId id) {
        return loadUser.load(id).orElseThrow(() -> new NotFoundException("User " + id.value() + " not found."));
    }
}
