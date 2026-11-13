package com.cleanlearning.domain.offering.value;

import java.util.Objects;
import java.util.UUID;

public record ConsultationPackageId(UUID value) {

  public ConsultationPackageId {
    Objects.requireNonNull(value, "value");
  }
}
