package com.cleanlearning.adapters.database.offering.row;

import java.math.BigDecimal;
import java.util.UUID;

import com.cleanlearning.domain.offering.value.OfferingType;

/**
 * One row per offering and package: the offering's shop fields and one package price.
 */
public record ShopOfferingPriceRow(UUID id, OfferingType type, String name, String description, BigDecimal price) {
}
