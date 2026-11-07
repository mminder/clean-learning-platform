package com.cleanlearning.adapters.database.offering;

import java.math.BigDecimal;
import java.util.UUID;

import com.cleanlearning.domain.offering.OfferingType;

/**
 * One row per offering and package: the offering's shop fields and one package price.
 */
record ShopOfferingPriceRow(UUID id, OfferingType type, String name, String description, BigDecimal price) {
}
