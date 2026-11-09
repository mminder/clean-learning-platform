package com.cleanlearning.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.cleanlearning.domain.user.value.Role;
import com.cleanlearning.domain.user.value.UserId;

class UserTest {

    private static final UserId ID = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000202"));

    @Test
    void isTheCurrentUserWithItsIdAndRolesAfterLogin() {
        User user = new User(ID, "Expert 2", "expert2@example.com", Set.of(Role.EXPERT, Role.ADMIN));

        assertThat(user.toCurrentUser()).isEqualTo(new CurrentUser(ID, Set.of(Role.EXPERT, Role.ADMIN)));
    }

    @Test
    void keepsItsRolesWhenTheGivenSetChanges() {
        Set<Role> roles = new HashSet<>(Set.of(Role.CUSTOMER));
        User user = new User(ID, "Customer 1", "customer1@example.com", roles);

        roles.add(Role.ADMIN);

        assertThat(user.roles()).containsExactly(Role.CUSTOMER);
        assertThatThrownBy(() -> user.roles().add(Role.ADMIN)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void currentUserKeepsItsRolesWhenTheGivenSetChanges() {
        Set<Role> roles = new HashSet<>(Set.of(Role.CUSTOMER));
        CurrentUser currentUser = new CurrentUser(ID, roles);

        roles.add(Role.ADMIN);

        assertThat(currentUser.roles()).containsExactly(Role.CUSTOMER);
    }

    @Test
    void needsAllItsValues() {
        assertThatThrownBy(() -> new User(null, "Admin 1", "admin1@example.com", Set.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, null, "admin1@example.com", Set.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, "Admin 1", null, Set.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, "Admin 1", "admin1@example.com", null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CurrentUser(null, Set.of())).isInstanceOf(NullPointerException.class);
    }
}
