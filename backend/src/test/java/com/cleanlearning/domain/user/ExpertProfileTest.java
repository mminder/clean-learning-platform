package com.cleanlearning.domain.user;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpertProfileTest {

  @Test
  void needsAllItsValues() {
    UserId id = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000201"));
    DisplayName displayName = new DisplayName("Expert 1");
    assertThatNullPointerException().isThrownBy(() -> new ExpertProfile(null, displayName, "Bio"));
    assertThatNullPointerException().isThrownBy(() -> new ExpertProfile(id, null, "Bio"));
    assertThatNullPointerException().isThrownBy(() -> new ExpertProfile(id, displayName, null));
    assertThatNullPointerException().isThrownBy(() -> new UserId(null));
  }
}
