package com.cleanlearning.domain.consultation.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class BusyTimeTest {

  private static final Instant TEN = Instant.parse("2026-11-17T09:00:00Z");
  private static final Instant ELEVEN = Instant.parse("2026-11-17T10:00:00Z");

  @Test
  void holdsItsStartAndEnd() {
    BusyTime busyTime = new BusyTime(TEN, ELEVEN);

    assertThat(busyTime.start()).isEqualTo(TEN);
    assertThat(busyTime.end()).isEqualTo(ELEVEN);
  }

  @Test
  void endsAfterItStarts() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new BusyTime(ELEVEN, TEN))
        .withMessage("Busy time ends after it starts: " + ELEVEN + " to " + TEN + ".");
    assertThatIllegalArgumentException().isThrownBy(() -> new BusyTime(TEN, TEN));
  }

  @Test
  void needsBothInstants() {
    assertThatNullPointerException()
        .isThrownBy(() -> new BusyTime(null, ELEVEN))
        .withMessage("start");
    assertThatNullPointerException().isThrownBy(() -> new BusyTime(TEN, null)).withMessage("end");
  }
}
