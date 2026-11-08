package com.cleanlearning.application.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.domain.user.User;

/**
 * UC-01 PoC step 2: the seeded users to pick from, with their roles, sorted by name.
 */
@Service
public class ListPocUsersUseCase {

    private final LoadUsersPort loadUsers;

    ListPocUsersUseCase(LoadUsersPort loadUsers) {
        this.loadUsers = loadUsers;
    }

    @Transactional(readOnly = true)
    public List<User> invoke() {
        return loadUsers.loadAll();
    }
}
