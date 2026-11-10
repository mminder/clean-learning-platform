package com.cleanlearning.api.consultation.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * The body of a booking (UC-06 step 6): the expert the dialog showed, the one the customer picked or the case's, and
 * the start of the slot they confirmed, an instant. The server books only with this expert (spec 04, decision 12), and
 * only at a bookable slot.
 */
public record BookConsultationAppointmentRequest(@NotNull UUID expertId, @NotNull Instant start) {
}
