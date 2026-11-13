package com.cleanlearning.adapters.database.offering.row;

import com.cleanlearning.domain.offering.value.OfferingType;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * One row per offering and package: the offering's shop fields and one package price. The image id
 * is {@code null} without an image.
 */
public record ShopOfferingPriceRow(
    UUID id,
    OfferingType type,
    String name,
    String description,
    @Nullable UUID imageId,
    BigDecimal price) {}
