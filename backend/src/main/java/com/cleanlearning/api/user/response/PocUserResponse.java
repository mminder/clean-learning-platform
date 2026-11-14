package com.cleanlearning.api.user.response;

import com.cleanlearning.domain.user.User;
import com.cleanlearning.domain.user.value.Role;
import java.util.List;
import java.util.UUID;

public record PocUserResponse(UUID id, String name, List<Role> roles) {

  public static PocUserResponse from(User user) {
    return new PocUserResponse(
        user.id().value(), user.name(), user.roles().stream().sorted().toList());
  }
}
