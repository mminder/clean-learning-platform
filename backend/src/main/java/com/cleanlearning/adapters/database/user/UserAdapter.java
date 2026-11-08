package com.cleanlearning.adapters.database.user;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.user.row.UserRoleRow;
import com.cleanlearning.application.user.LoadExpertsPort;
import com.cleanlearning.application.user.LoadUserPort;
import com.cleanlearning.application.user.LoadUsersPort;
import com.cleanlearning.domain.user.Expert;
import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;

@Component
class UserAdapter implements LoadUsersPort, LoadUserPort, LoadExpertsPort {

    private final UserRepository userRepository;

    UserAdapter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> loadAll() {
        // The rows come sorted by name; the map keeps that order and groups each user's roles.
        Map<UUID, List<UserRoleRow>> rowsByUser = new LinkedHashMap<>();
        for (UserRoleRow row : userRepository.findAllWithRoles()) {
            rowsByUser.computeIfAbsent(row.id(), id -> new ArrayList<>()).add(row);
        }
        return rowsByUser.values().stream().map(UserAdapter::toUser).toList();
    }

    @Override
    public Optional<User> load(UserId id) {
        List<UserRoleRow> rows = userRepository.findWithRoles(id.value());
        return rows.isEmpty() ? Optional.empty() : Optional.of(toUser(rows));
    }

    @Override
    public List<Expert> loadExperts() {
        return userRepository.findExperts().stream()
                .map(row -> new Expert(new UserId(row.id()), new DisplayName(row.displayName()))).toList();
    }

    private static User toUser(List<UserRoleRow> rows) {
        UserRoleRow first = rows.getFirst();
        return new User(new UserId(first.id()), first.name(), first.email(),
                rows.stream().map(UserRoleRow::role).filter(Objects::nonNull).collect(Collectors.toSet()));
    }
}
