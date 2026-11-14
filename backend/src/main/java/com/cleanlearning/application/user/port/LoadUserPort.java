package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LoadUserPort {

  Optional<User> load(UserId id);
}
