package com.cleanlearning.domain.offering.value;

import com.cleanlearning.domain.shared.InvalidValueException;

public record SessionDuration(int minutes) {

  public SessionDuration {
    if (minutes <= 0) {
      throw new InvalidValueException("A session lasts at least one minute: " + minutes);
    }
  }
}
