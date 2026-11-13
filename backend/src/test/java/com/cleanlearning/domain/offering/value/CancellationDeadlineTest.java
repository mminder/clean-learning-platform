package com.cleanlearning.domain.offering.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CancellationDeadlineTest {

  @Test
  void isANumberOfHours() {
    assertThat(new CancellationDeadline(24).hours()).isEqualTo(24);
  }

  @Test
  void acceptsZeroHours() {
    // No deadline: cancelling is free until the session starts.
    assertThat(new CancellationDeadline(0).hours()).isZero();
  }

  @Test
  void rejectsNegativeHours() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CancellationDeadline(-1))
        .withMessageContaining("-1");
  }

  @Test
  void isComparedByValue() {
    assertThat(new CancellationDeadline(24)).isEqualTo(new CancellationDeadline(24));
    assertThat(new CancellationDeadline(24)).isNotEqualTo(new CancellationDeadline(48));
  }

  @Test
  void cancellingIsFreeUntilTheDeadlineBeforeTheStart() {
    Instant start = Instant.parse("2026-11-17T09:00:00Z");

    assertThat(new CancellationDeadline(24).freeUntil(start))
        .isEqualTo(Instant.parse("2026-11-16T09:00:00Z"));
    assertThat(new CancellationDeadline(168).freeUntil(start))
        .isEqualTo(Instant.parse("2026-11-10T09:00:00Z"));
  }

  @Test
  void withoutADeadlineCancellingIsFreeUntilTheStart() {
    Instant start = Instant.parse("2026-11-17T09:00:00Z");

    assertThat(new CancellationDeadline(0).freeUntil(start)).isEqualTo(start);
  }

  @Test
  void countsRealHoursAcrossTheChangeToSummerTime() {
    // Sun 28 Mar 2027, 12:00 in Zurich (UTC+2); 24 real hours before is Sat 27 Mar, 11:00 (UTC+1),
    // not 12:00.
    assertThat(new CancellationDeadline(24).freeUntil(Instant.parse("2027-03-28T10:00:00Z")))
        .isEqualTo(Instant.parse("2027-03-27T10:00:00Z"));
  }
}
