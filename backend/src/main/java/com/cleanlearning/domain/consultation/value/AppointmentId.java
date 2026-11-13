package com.cleanlearning.domain.consultation.value;

import java.util.Objects;
import java.util.UUID;

public record AppointmentId(UUID value) {

  public AppointmentId {
    Objects.requireNonNull(value, "value");
  }
}
