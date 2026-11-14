package com.cleanlearning.application.user;

import com.cleanlearning.application.user.port.LoadUserPort;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCurrentUserUseCase {

  private final LoadUserPort loadUser;

  GetCurrentUserUseCase(LoadUserPort loadUser) {
    this.loadUser = loadUser;
  }

  @Transactional(readOnly = true)
  public User invoke(CurrentUser currentUser) {
    return loadUser
        .load(currentUser.id())
        .orElseThrow(
            () -> new NotFoundException("User " + currentUser.id().value() + " not found."));
  }
}
