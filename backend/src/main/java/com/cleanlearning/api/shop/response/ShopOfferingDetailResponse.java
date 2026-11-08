package com.cleanlearning.api.shop.response;

import java.util.UUID;

import com.cleanlearning.domain.offering.ShopConsultationOffering;
import com.cleanlearning.domain.offering.value.OfferingType;

/**
 * An offering's detail page in the shop. Trainings will add a {@code training} part next to {@code consultation}.
 */
public record ShopOfferingDetailResponse(UUID id, OfferingType type, String name, String description,
        ConsultationOfferingDetailResponse consultation) {

    public static ShopOfferingDetailResponse from(ShopConsultationOffering offering) {
        return new ShopOfferingDetailResponse(offering.id().value(), offering.type(), offering.name(),
                offering.description(), ConsultationOfferingDetailResponse.from(offering));
    }
}
