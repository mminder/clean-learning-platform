package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.view.CustomerConsultationCase;
import com.cleanlearning.domain.image.value.ImageId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A case as its customer sees it (UC-05's case detail). The offering's name and image are live; the
 * image id is {@code null} without an image, and {@code GET /api/images/{id}} serves it. {@code
 * purchasedAt} is an instant. The expert is {@code null} before the first booking. The upcoming
 * appointments haven't ended, soonest first, a running one {@code CONSUMED}, each with its
 * cancellation preview; the past ones have, newest first. Neither lists a cancelled appointment
 * (spec 05, decision 3). {@code cancellation} is {@code null} for an active case; {@code
 * canCancel}: the case is active and not completed; {@code refundIfCancelledNow} is {@code null}
 * exactly when {@code canCancel} is false; {@code canBook}: the case is active and has an open
 * credit. The server computes the credits, which lack the forfeited ones, the statuses, the
 * previews, {@code canCancel} and {@code canBook} (spec 04, decision 18).
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
        consultationCase.openCredits(),
        consultationCase.totalCredits(),
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
