package com.cleanlearning.adapters.database.offering;

import java.math.BigDecimal;
import java.util.UUID;

record ConsultationPackageRow(UUID id, int sessionCount, BigDecimal price) {
}
