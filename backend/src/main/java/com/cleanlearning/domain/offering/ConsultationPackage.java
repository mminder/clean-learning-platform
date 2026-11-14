package com.cleanlearning.domain.offering;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;
import java.util.Objects;

public record ConsultationPackage(
    ConsultationPackageId id, SessionCount sessionCount, Money price) {

  public ConsultationPackage {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(sessionCount, "sessionCount");
    Objects.requireNonNull(price, "price");
  }
}
