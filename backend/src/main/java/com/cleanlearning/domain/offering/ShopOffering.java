package com.cleanlearning.domain.offering;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.offering.value.OfferingType;
import com.cleanlearning.domain.shared.value.Money;

/**
 * An offering as a card in the shop (UC-02). The price is what the card shows: for a consultation offering, the price
 * of its cheapest package, shown as "from".
 */
public record ShopOffering(OfferingId id, OfferingType type, String name, String description, Money price) {

    public ShopOffering {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(price, "price");
    }

    public static ShopOffering consultation(OfferingId id, String name, String description,
            List<Money> packagePrices) {
        if (packagePrices.isEmpty()) {
            throw new IllegalArgumentException("A consultation offering needs at least one package: " + id);
        }
        return new ShopOffering(id, OfferingType.CONSULTATION, name, description, Collections.min(packagePrices));
    }
}
