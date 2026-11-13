package com.cleanlearning.domain.image.value;

import java.util.Objects;
import java.util.UUID;

public record ImageId(UUID value) {

  public ImageId {
    Objects.requireNonNull(value, "value");
  }
}
