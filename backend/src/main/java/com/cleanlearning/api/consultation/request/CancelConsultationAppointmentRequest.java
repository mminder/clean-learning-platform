package com.cleanlearning.api.consultation.request;

import jakarta.validation.constraints.NotNull;

/**
 * The body of an appointment's cancellation (UC-07 step 3): whether the dialog said the credit
 * would be returned. The server cancels only with this outcome; another one answers 409 {@code
 * DEADLINE_PASSED} (spec 05, decision 4).
 */
public record CancelConsultationAppointmentRequest(@NotNull Boolean creditRefunded) {}
