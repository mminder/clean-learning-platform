package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.view.ExpertConsultationCase;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * The case as its assigned expert sees it: the terms without the price paid, and {@code
 * cancelledAt} without the refund outcome (D-18); {@code cancelledAt} is {@code null} for an active
 * case. The server computes the lists and the statuses. The notes are every note, private and
 * public, newest first.
 */
public record ExpertConsultationCaseResponse(
    UUID id,
    String customerName,
    String customerEmail,
    String offeringName,
    int sessionCount,
    int sessionDurationMinutes,
    int cancellationDeadlineHours,
    Instant purchasedAt,
    int openCredits,
    int totalCredits,
    @Nullable Instant cancelledAt,
    List<ExpertAppointmentResponse> upcomingAppointments,
    List<ExpertAppointmentResponse> pastAppointments,
    List<ExpertCancelledAppointmentResponse> cancelledAppointments,
    List<ExpertNoteResponse> notes) {

  public static ExpertConsultationCaseResponse from(ExpertConsultationCase consultationCase) {
    return new ExpertConsultationCaseResponse(
        consultationCase.id().value(),
        consultationCase.customerName(),
        consultationCase.customerEmail(),
        consultationCase.offeringName().value(),
        consultationCase.credits().sessionCount().value(),
        consultationCase.sessionDuration().minutes(),
        consultationCase.cancellationDeadline().hours(),
        consultationCase.purchasedAt(),
        consultationCase.credits().openCredits(),
        consultationCase.credits().totalCredits(),
        consultationCase.cancelledAt().orElse(null),
        consultationCase.upcomingAppointments().stream()
            .map(ExpertAppointmentResponse::from)
            .toList(),
        consultationCase.pastAppointments().stream().map(ExpertAppointmentResponse::from).toList(),
        consultationCase.cancelledAppointments().stream()
            .map(ExpertCancelledAppointmentResponse::from)
            .toList(),
        consultationCase.notes().stream().map(ExpertNoteResponse::from).toList());
  }
}
