package com.cleanlearning.domain.availability.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.shared.InvalidValueException;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class TimeRangeTest {

  private static TimeRange range(String start, String end) {
    return new TimeRange(LocalTime.parse(start), LocalTime.parse(end));
  }

  @Test
  void startsBeforeItEnds() {
    TimeRange morning = range("09:00", "12:00");

    assertThat(morning.start()).isEqualTo(LocalTime.of(9, 0));
    assertThat(morning.end()).isEqualTo(LocalTime.of(12, 0));
  }

  @Test
  void mayStartAndEndAtAnyMinute() {
    assertThat(range("09:07", "09:08").end()).isEqualTo(LocalTime.of(9, 8));
    assertThat(range("00:00", "23:59").start()).isEqualTo(LocalTime.MIDNIGHT);
  }

  @Test
  void rejectsAnEndThatIsNotAfterTheStart() {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> range("12:00", "09:00"))
        .withMessage("A time range ends after it starts: 12:00–09:00.");
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> range("09:00", "09:00"))
        .withMessage("A time range ends after it starts: 09:00–09:00.");
  }

  @Test
  void rejectsAMissingTime() {
    assertThatNullPointerException().isThrownBy(() -> new TimeRange(null, LocalTime.NOON));
    assertThatNullPointerException().isThrownBy(() -> new TimeRange(LocalTime.NOON, null));
  }

  @Test
  void overlapsOrTouchesARangeItSharesATimeWith() {
    TimeRange morning = range("09:00", "12:00");

    assertThat(morning.overlapsOrTouches(range("11:00", "14:00"))).isTrue();
    assertThat(morning.overlapsOrTouches(range("07:00", "09:30"))).isTrue();
    assertThat(morning.overlapsOrTouches(range("10:00", "11:00"))).isTrue();
    assertThat(morning.overlapsOrTouches(range("08:00", "13:00"))).isTrue();
    assertThat(morning.overlapsOrTouches(morning)).isTrue();
  }

  @Test
  void touchesARangeThatStartsWhereItEnds() {
    TimeRange morning = range("09:00", "12:00");

    assertThat(morning.overlapsOrTouches(range("12:00", "15:00"))).isTrue();
    assertThat(morning.overlapsOrTouches(range("06:00", "09:00"))).isTrue();
  }

  @Test
  void neitherOverlapsNorTouchesARangeWithAGap() {
    TimeRange morning = range("09:00", "12:00");

    assertThat(morning.overlapsOrTouches(range("12:01", "15:00"))).isFalse();
    assertThat(morning.overlapsOrTouches(range("06:00", "08:59"))).isFalse();
  }

  @Test
  void isComparedByValue() {
    assertThat(range("09:00", "12:00")).isEqualTo(range("09:00", "12:00"));
    assertThat(range("09:00", "12:00")).isNotEqualTo(range("09:00", "12:30"));
  }

  @Test
  void showsAsStartToEnd() {
    assertThat(range("09:00", "12:00")).hasToString("09:00–12:00");
  }
}
