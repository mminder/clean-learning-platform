package com.cleanlearning.adapters.database.offering.row;

import com.cleanlearning.domain.offering.value.OfferingType;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ShopOfferingPriceRow(
    UUID id,
    OfferingType type,
    String name,
    String description,
    @Nullable UUID imageId,
    BigDecimal price) {}
