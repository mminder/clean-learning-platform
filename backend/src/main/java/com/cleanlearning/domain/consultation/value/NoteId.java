package com.cleanlearning.domain.consultation.value;

import java.util.Objects;
import java.util.UUID;

public record NoteId(UUID value) {

  public NoteId {
    Objects.requireNonNull(value, "value");
  }
}
