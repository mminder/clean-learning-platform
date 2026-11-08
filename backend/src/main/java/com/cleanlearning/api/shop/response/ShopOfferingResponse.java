package com.cleanlearning.api.shop.response;

import java.util.UUID;

import com.cleanlearning.api.response.MoneyResponse;
import com.cleanlearning.domain.offering.ShopOffering;
import com.cleanlearning.domain.offering.value.OfferingType;

/**
 * An offering card in the shop. For a consultation offering, the price is its cheapest package ("from").
 */
public record ShopOfferingResponse(UUID id, OfferingType type, String name, String description,
        MoneyResponse price) {

    public static ShopOfferingResponse from(ShopOffering offering) {
        return new ShopOfferingResponse(offering.id().value(), offering.type(), offering.name(),
                offering.description(), MoneyResponse.from(offering.price()));
    }
}
