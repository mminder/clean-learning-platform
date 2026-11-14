package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.image.value.ImageId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * {@code cancellation} is {@code null} for an active case; {@code refundIfCancelledNow} is {@code
 * null} exactly when {@code canCancel} is false. The server computes everything that depends on the
 * time.
 */
public record CustomerConsultationCaseResponse(
    UUID id,
    String offeringName,
    @Nullable UUID offeringImageId,
    PurchaseTermsResponse purchaseTerms,
    Instant purchasedAt,
    int openCredits,
    int totalCredits,
    @Nullable AssignedExpertResponse expert,
    List<CustomerUpcomingAppointmentResponse> upcomingAppointments,
    List<CustomerAppointmentResponse> pastAppointments,
    @Nullable CancellationOutcomeResponse cancellation,
    boolean canCancel,
    @Nullable RefundEligibilityResponse refundIfCancelledNow,
    boolean canBook) {

  public static CustomerConsultationCaseResponse from(CustomerConsultationCase consultationCase) {
    return new CustomerConsultationCaseResponse(
        consultationCase.id().value(),
        consultationCase.offeringName().value(),
        consultationCase.offeringImageId().map(ImageId::value).orElse(null),
        PurchaseTermsResponse.from(consultationCase.purchaseTerms()),
        consultationCase.purchasedAt(),
        consultationCase.credits().openCredits(),
        consultationCase.credits().totalCredits(),
        consultationCase.expert().map(AssignedExpertResponse::from).orElse(null),
        consultationCase.upcomingAppointments().stream()
            .map(CustomerUpcomingAppointmentResponse::from)
            .toList(),
        consultationCase.pastAppointments().stream()
            .map(CustomerAppointmentResponse::from)
            .toList(),
        consultationCase.cancellation().map(CancellationOutcomeResponse::from).orElse(null),
        consultationCase.canCancel(),
        consultationCase.refundIfCancelledNow().map(RefundEligibilityResponse::from).orElse(null),
        consultationCase.canBook());
  }
}
