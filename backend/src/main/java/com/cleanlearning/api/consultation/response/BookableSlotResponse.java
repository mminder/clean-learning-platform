package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.value.BookableSlot;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** {@code freeCancellationUntil} is {@code null} when that time has already passed. */
public record BookableSlotResponse(
    Instant start, Instant end, @Nullable Instant freeCancellationUntil) {

  public static BookableSlotResponse from(BookableSlot slot) {
    return new BookableSlotResponse(
        slot.start(), slot.end(), slot.freeCancellationUntil().orElse(null));
  }
}
