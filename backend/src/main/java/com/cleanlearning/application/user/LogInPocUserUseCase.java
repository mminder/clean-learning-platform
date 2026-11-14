package com.cleanlearning.application.user;

import com.cleanlearning.application.user.port.LoadUserPort;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogInPocUserUseCase {

  private final LoadUserPort loadUser;

  LogInPocUserUseCase(LoadUserPort loadUser) {
    this.loadUser = loadUser;
  }

  @Transactional(readOnly = true)
  public User invoke(UserId id) {
    return loadUser
        .load(id)
        .orElseThrow(() -> new NotFoundException("User " + id.value() + " not found."));
  }
}
