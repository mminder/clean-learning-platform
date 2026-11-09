package com.cleanlearning.api.consultation.response;

import java.time.Instant;

import com.cleanlearning.domain.consultation.value.BookableSlot;

/**
 * A time the customer can book (UC-06 steps 3–4): the session's start and end, and until when it can be cancelled for
 * free, which the confirmation states (UC-06 step 5). All three are instants; the server computes them (spec 04,
 * decision 18).
 */
public record BookableSlotResponse(Instant start, Instant end, Instant freeCancellationUntil) {

    public static BookableSlotResponse from(BookableSlot slot) {
        return new BookableSlotResponse(slot.start(), slot.end(), slot.freeCancellationUntil());
    }
}
