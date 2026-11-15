package com.cleanlearning.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpertProfileTest {

  private static final UserId ID =
      new UserId(UUID.fromString("00000000-0000-0000-0000-000000000201"));

  private static final DisplayName DISPLAY_NAME = new DisplayName("Expert 1");

  @Test
  void hasAPhotoOrNone() {
    ImageId photo = new ImageId(UUID.fromString("00000000-0000-0000-0000-000000009001"));

    assertThat(new ExpertProfile(ID, DISPLAY_NAME, "Bio", Optional.of(photo)).photoId())
        .contains(photo);
    assertThat(new ExpertProfile(ID, DISPLAY_NAME, "Bio", Optional.empty()).photoId()).isEmpty();
  }

  @Test
  void needsAllItsValues() {
    Optional<ImageId> noPhoto = Optional.empty();
    assertThatNullPointerException()
        .isThrownBy(() -> new ExpertProfile(null, DISPLAY_NAME, "Bio", noPhoto));
    assertThatNullPointerException().isThrownBy(() -> new ExpertProfile(ID, null, "Bio", noPhoto));
    assertThatNullPointerException()
        .isThrownBy(() -> new ExpertProfile(ID, DISPLAY_NAME, null, noPhoto));
    assertThatNullPointerException()
        .isThrownBy(() -> new ExpertProfile(ID, DISPLAY_NAME, "Bio", null));
    assertThatNullPointerException().isThrownBy(() -> new UserId(null));
  }
}
