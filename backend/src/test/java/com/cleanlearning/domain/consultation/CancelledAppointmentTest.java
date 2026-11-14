package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** A cancelled appointment as its case keeps it (glossary). Instants are in UTC. */
class CancelledAppointmentTest {

  private static final AppointmentId ID = new AppointmentId(UUID.randomUUID());

  /** Tue 17 Nov 2026, 10:00–11:00 in Zurich. */
  private static final Instant START = Instant.parse("2026-11-17T09:00:00Z");

  private static final Instant END = Instant.parse("2026-11-17T10:00:00Z");

  @Test
  void cancelledLateItsCreditIsForfeitedInTimeItIsNot() {
    assertThat(new CancelledAppointment(ID, START, END, false).isCreditForfeited()).isTrue();
    assertThat(new CancelledAppointment(ID, START, END, true).isCreditForfeited()).isFalse();
  }

  @Test
  void endsAfterItStarts() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CancelledAppointment(ID, END, START, true))
        .withMessage("An appointment ends after it starts: " + END + " to " + START + ".");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new CancelledAppointment(ID, START, START, true));
  }

  @Test
  void needsAllItsFields() {
    assertThatNullPointerException()
        .isThrownBy(() -> new CancelledAppointment(null, START, END, true))
        .withMessage("id");
    assertThatNullPointerException()
        .isThrownBy(() -> new CancelledAppointment(ID, null, END, true))
        .withMessage("start");
    assertThatNullPointerException()
        .isThrownBy(() -> new CancelledAppointment(ID, START, null, true))
        .withMessage("end");
  }
}
