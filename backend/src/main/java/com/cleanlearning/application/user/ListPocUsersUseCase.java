package com.cleanlearning.application.user;

import com.cleanlearning.application.user.port.LoadUsersPort;
import com.cleanlearning.domain.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
