package com.cleanlearning.domain.consultation.outcome;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import java.util.Objects;

public record AppointmentCancellation(AppointmentId appointmentId, boolean creditRefunded) {

  public AppointmentCancellation {
    Objects.requireNonNull(appointmentId, "appointmentId");
  }
}
