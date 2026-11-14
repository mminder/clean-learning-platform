package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.value.DisplayName;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The server computes {@code group}, and the answer comes in the order of "My purchases". */
public record CustomerConsultationCaseSummaryResponse(
    UUID id,
    String offeringName,
    @Nullable UUID offeringImageId,
    Instant purchasedAt,
    int openCredits,
    int totalCredits,
    @Nullable String expertDisplayName,
    @Nullable CustomerAppointmentResponse nextAppointment,
    PurchaseGroup group,
    @Nullable Instant cancelledAt) {

  public static CustomerConsultationCaseSummaryResponse from(
      CustomerConsultationCaseSummary consultationCase) {
    CaseInList inList = consultationCase.inList();
    return new CustomerConsultationCaseSummaryResponse(
        inList.id().value(),
        consultationCase.offeringName().value(),
        consultationCase.offeringImageId().map(ImageId::value).orElse(null),
        inList.purchasedAt(),
        inList.credits().openCredits(),
        inList.credits().totalCredits(),
        consultationCase.expertDisplayName().map(DisplayName::value).orElse(null),
        consultationCase.nextAppointment().map(CustomerAppointmentResponse::from).orElse(null),
        consultationCase.group(),
        inList.cancelledAt().orElse(null));
  }
}
