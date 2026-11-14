package com.cleanlearning.domain.consultation.outcome;

import com.cleanlearning.domain.consultation.Appointment;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Objects;

public record Booking(Appointment appointment, UserId expertId, boolean assignsExpert) {

  public Booking {
    Objects.requireNonNull(appointment, "appointment");
    Objects.requireNonNull(expertId, "expertId");
  }
}
