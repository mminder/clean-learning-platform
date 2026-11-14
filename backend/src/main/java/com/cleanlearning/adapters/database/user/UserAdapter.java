package com.cleanlearning.adapters.database.user;

import com.cleanlearning.adapters.database.user.entity.UserEntity;
import com.cleanlearning.adapters.database.user.row.UserRoleRow;
import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.application.user.port.LoadUserPort;
import com.cleanlearning.application.user.port.LoadUsersPort;
import com.cleanlearning.application.user.port.LockExpertPort;
import com.cleanlearning.domain.user.Expert;
import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class UserAdapter implements LoadUsersPort, LoadUserPort, LoadExpertsPort, LockExpertPort {

  private final UserRepository userRepository;

  private final EntityManager entityManager;

  UserAdapter(UserRepository userRepository, EntityManager entityManager) {
    this.userRepository = userRepository;
    this.entityManager = entityManager;
  }

  @Override
  public List<User> loadAll() {
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
        .map(row -> new Expert(new UserId(row.id()), new DisplayName(row.displayName())))
        .toList();
  }

  @Override
  public void lock(UserId expertId) {
    if (entityManager.find(UserEntity.class, expertId.value(), LockModeType.PESSIMISTIC_WRITE)
        == null) {
      throw new IllegalStateException("User " + expertId.value() + " not found.");
    }
  }

  private static User toUser(List<UserRoleRow> rows) {
    UserRoleRow first = rows.getFirst();
    return new User(
        new UserId(first.id()),
        first.name(),
        first.email(),
        rows.stream().map(UserRoleRow::role).filter(Objects::nonNull).collect(Collectors.toSet()));
  }
}
