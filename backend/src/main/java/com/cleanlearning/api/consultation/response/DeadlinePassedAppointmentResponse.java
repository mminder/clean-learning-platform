package com.cleanlearning.api.consultation.response;

import java.time.Instant;

/**
 * The soonest appointment whose free cancellation has passed, which keeps a case from being refund-eligible (UC-08's
 * third reason): its start and end, and its free-cancellation time, all instants.
 */
public record DeadlinePassedAppointmentResponse(Instant start, Instant end, Instant freeCancellationUntil) {
}
