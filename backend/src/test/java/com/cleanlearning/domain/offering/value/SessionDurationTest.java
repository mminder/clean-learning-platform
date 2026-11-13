package com.cleanlearning.domain.offering.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class SessionDurationTest {

  @Test
  void isAPositiveNumberOfMinutes() {
    assertThat(new SessionDuration(60).minutes()).isEqualTo(60);
    assertThat(new SessionDuration(1).minutes()).isEqualTo(1);
  }

  @Test
  void rejectsZeroAndNegativeMinutes() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SessionDuration(0))
        .withMessageContaining("0");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new SessionDuration(-30))
        .withMessageContaining("-30");
  }

  @Test
  void isComparedByValue() {
    assertThat(new SessionDuration(60)).isEqualTo(new SessionDuration(60));
    assertThat(new SessionDuration(60)).isNotEqualTo(new SessionDuration(90));
  }
}
