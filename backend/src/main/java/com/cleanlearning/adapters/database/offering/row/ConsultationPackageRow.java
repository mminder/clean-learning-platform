package com.cleanlearning.adapters.database.offering.row;

import java.math.BigDecimal;
import java.util.UUID;

public record ConsultationPackageRow(UUID id, int sessionCount, BigDecimal price) {}
