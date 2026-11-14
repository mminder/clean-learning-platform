package com.cleanlearning.domain.offering.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.cleanlearning.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;

class SessionCountTest {

  @Test
  void isAPositiveNumberOfSessions() {
    assertThat(new SessionCount(3).value()).isEqualTo(3);
    assertThat(new SessionCount(1).value()).isEqualTo(1);
  }

  @Test
  void rejectsZeroAndNegativeCounts() {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new SessionCount(0))
        .withMessageContaining("0");
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new SessionCount(-3))
        .withMessageContaining("-3");
  }

  @Test
  void isComparedByValue() {
    assertThat(new SessionCount(3)).isEqualTo(new SessionCount(3));
    assertThat(new SessionCount(3)).isNotEqualTo(new SessionCount(5));
  }
}
