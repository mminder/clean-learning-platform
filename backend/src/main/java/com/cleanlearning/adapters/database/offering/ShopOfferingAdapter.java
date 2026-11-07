package com.cleanlearning.adapters.database.offering;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.cleanlearning.application.offering.LoadShopOfferingsPort;
import com.cleanlearning.domain.offering.OfferingId;
import com.cleanlearning.domain.offering.ShopOffering;
import com.cleanlearning.domain.shared.Money;

@Component
class ShopOfferingAdapter implements LoadShopOfferingsPort {

    private final OfferingRepository offeringRepository;

    ShopOfferingAdapter(OfferingRepository offeringRepository) {
        this.offeringRepository = offeringRepository;
    }

    @Override
    public List<ShopOffering> loadPublished() {
        // The rows come sorted by offering name; the map keeps that order and groups each offering's prices.
        Map<UUID, List<ShopOfferingPriceRow>> rowsByOffering = new LinkedHashMap<>();
        for (ShopOfferingPriceRow row : offeringRepository.findPublishedWithPackagePrices()) {
            rowsByOffering.computeIfAbsent(row.id(), id -> new ArrayList<>()).add(row);
        }
        return rowsByOffering.values().stream().map(ShopOfferingAdapter::toShopOffering).toList();
    }

    private static ShopOffering toShopOffering(List<ShopOfferingPriceRow> rows) {
        ShopOfferingPriceRow first = rows.getFirst();
        List<Money> packagePrices = rows.stream().map(row -> new Money(row.price())).toList();
        return switch (first.type()) {
            case CONSULTATION -> ShopOffering.consultation(new OfferingId(first.id()), first.name(),
                    first.description(), packagePrices);
        };
    }
}
