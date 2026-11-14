package com.cleanlearning.domain.offering.value;

import com.cleanlearning.domain.shared.InvalidValueException;

public record SessionCount(int value) {

  public SessionCount {
    if (value <= 0) {
      throw new InvalidValueException("A package has at least one session: " + value);
    }
  }
}
