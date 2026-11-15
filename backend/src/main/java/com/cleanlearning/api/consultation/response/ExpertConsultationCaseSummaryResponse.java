package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.consultation.view.ExpertConsultationCaseSummary;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The server computes {@code group}, and the answer comes in the order of "My cases". */
public record ExpertConsultationCaseSummaryResponse(
    UUID id,
    String customerName,
    String offeringName,
    Instant purchasedAt,
    int openCredits,
    int totalCredits,
    @Nullable ExpertAppointmentResponse nextAppointment,
    PurchaseGroup group,
    @Nullable Instant cancelledAt) {

  public static ExpertConsultationCaseSummaryResponse from(
      ExpertConsultationCaseSummary consultationCase) {
    CaseInList inList = consultationCase.inList();
    return new ExpertConsultationCaseSummaryResponse(
        inList.id().value(),
        consultationCase.customerName(),
        consultationCase.offeringName().value(),
        inList.purchasedAt(),
        inList.credits().openCredits(),
        inList.credits().totalCredits(),
        consultationCase.nextAppointment().map(ExpertAppointmentResponse::from).orElse(null),
        consultationCase.group(),
        inList.cancelledAt().orElse(null));
  }
}
