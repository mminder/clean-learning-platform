package com.cleanlearning.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * UC-10's link window for customers: from one hour before the start until the end, so it includes
 * 09:00 and excludes 11:00 for the appointment 10:00–11:00 in Zurich (spec 06). Instants are in
 * UTC; Zurich is UTC+1 in November.
 */
class LinkWindowTest {

  /** Tue 17 Nov 2026, 10:00 and 11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  /** 09:00 in Zurich. */
  private static final Instant OPENS = Instant.parse("2026-11-17T08:00:00Z");

  @Test
  void opensOneHourBeforeTheStart() {
    assertThat(LinkWindow.OPENS_BEFORE_START).isEqualTo(Duration.ofHours(1));
    assertThat(LinkWindow.opensAt(START)).isEqualTo(OPENS);
  }

  @Test
  void isClosedJustBeforeItOpens() {
    assertThat(LinkWindow.isOpenAt(START, END, OPENS.minusNanos(1))).isFalse();
    assertThat(LinkWindow.isOpenAt(START, END, OPENS.minusSeconds(60))).isFalse();
  }

  @Test
  void isOpenFromOneHourBeforeTheStartWhileTheCallRuns() {
    assertThat(LinkWindow.isOpenAt(START, END, OPENS)).isTrue();
    assertThat(LinkWindow.isOpenAt(START, END, START)).isTrue();
    assertThat(LinkWindow.isOpenAt(START, END, END.minusNanos(1))).isTrue();
  }

  @Test
  void isClosedFromTheEnd() {
    assertThat(LinkWindow.isOpenAt(START, END, END)).isFalse();
    assertThat(LinkWindow.isOpenAt(START, END, END.plusSeconds(60))).isFalse();
  }
}
