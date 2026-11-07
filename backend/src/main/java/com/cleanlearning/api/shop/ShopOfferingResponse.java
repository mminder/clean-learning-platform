package com.cleanlearning.api.shop;

import java.util.UUID;

import com.cleanlearning.api.MoneyResponse;
import com.cleanlearning.domain.offering.OfferingType;
import com.cleanlearning.domain.offering.ShopOffering;

/**
 * An offering card in the shop. For a consultation offering, the price is its cheapest package ("from").
 */
public record ShopOfferingResponse(UUID id, OfferingType type, String name, String description,
        MoneyResponse price) {

    static ShopOfferingResponse from(ShopOffering offering) {
        return new ShopOfferingResponse(offering.id().value(), offering.type(), offering.name(),
                offering.description(), MoneyResponse.from(offering.price()));
    }
}
