package com.cleanlearning.api.consultation.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.image.value.ImageId;

/**
 * A case as its customer sees it (UC-05's case detail). The offering's name and image are live; the image id is
 * {@code null} without an image, and {@code GET /api/images/{id}} serves it. {@code purchasedAt} is an instant. The
 * expert is {@code null} before the first booking. The upcoming appointments haven't ended, soonest first, a running
 * one {@code CONSUMED}; the past ones have, newest first. The server computes the credits, the statuses and
 * {@code canBook} (spec 04, decision 18).
 */
public record CustomerConsultationCaseResponse(UUID id, String offeringName, @Nullable UUID offeringImageId,
        PurchaseTermsResponse purchaseTerms, Instant purchasedAt, int openCredits, int totalCredits,
        @Nullable AssignedExpertResponse expert, List<CustomerAppointmentResponse> upcomingAppointments,
        List<CustomerAppointmentResponse> pastAppointments, boolean canBook) {

    public static CustomerConsultationCaseResponse from(CustomerConsultationCase consultationCase) {
        return new CustomerConsultationCaseResponse(consultationCase.id().value(),
                consultationCase.offeringName().value(),
                consultationCase.offeringImageId().map(ImageId::value).orElse(null),
                PurchaseTermsResponse.from(consultationCase.purchaseTerms()), consultationCase.purchasedAt(),
                consultationCase.openCredits(), consultationCase.totalCredits(),
                consultationCase.expert().map(AssignedExpertResponse::from).orElse(null),
                consultationCase.upcomingAppointments().stream().map(CustomerAppointmentResponse::from).toList(),
                consultationCase.pastAppointments().stream().map(CustomerAppointmentResponse::from).toList(),
                consultationCase.canBook());
    }
}
