package com.cleanlearning.api.shop.response;

import java.util.List;

import com.cleanlearning.api.response.ConsultationPackageResponse;
import com.cleanlearning.domain.offering.ShopConsultationOffering;

/**
 * The consultation part of an offering's detail: packages fewest sessions first, eligible experts by display name.
 */
public record ConsultationOfferingDetailResponse(int sessionDurationMinutes, int cancellationDeadlineHours,
        List<ConsultationPackageResponse> packages, List<EligibleExpertResponse> eligibleExperts) {

    public static ConsultationOfferingDetailResponse from(ShopConsultationOffering offering) {
        return new ConsultationOfferingDetailResponse(offering.sessionDuration().minutes(),
                offering.cancellationDeadline().hours(),
                offering.packages().stream().map(ConsultationPackageResponse::from).toList(),
                offering.eligibleExperts().stream().map(EligibleExpertResponse::from).toList());
    }
}
