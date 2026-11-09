package com.cleanlearning.api.consultation.response;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.cleanlearning.domain.consultation.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.value.DisplayName;

/**
 * A case on "My purchases" (UC-05). The offering's name and image are live; the image id is {@code null} without an
 * image, and {@code GET /api/images/{id}} serves it. {@code purchasedAt} is an instant. {@code expertDisplayName} is
 * {@code null} until the first booking; {@code nextAppointment} is the soonest appointment that hasn't ended,
 * {@code CONSUMED} while it runs, and {@code null} without one. The server computes {@code group}, and the answer comes
 * in the order of "My purchases" (spec 04, decision 18).
 */
public record CustomerConsultationCaseSummaryResponse(UUID id, String offeringName,
        @Nullable UUID offeringImageId, Instant purchasedAt, int openCredits, int totalCredits,
        @Nullable String expertDisplayName, @Nullable CustomerAppointmentResponse nextAppointment,
        PurchaseGroup group) {

    public static CustomerConsultationCaseSummaryResponse from(CustomerConsultationCaseSummary consultationCase) {
        return new CustomerConsultationCaseSummaryResponse(consultationCase.id().value(),
                consultationCase.offeringName().value(),
                consultationCase.offeringImageId().map(ImageId::value).orElse(null), consultationCase.purchasedAt(),
                consultationCase.openCredits(), consultationCase.totalCredits(),
                consultationCase.expertDisplayName().map(DisplayName::value).orElse(null),
                consultationCase.nextAppointment().map(CustomerAppointmentResponse::from).orElse(null),
                consultationCase.group());
    }
}
