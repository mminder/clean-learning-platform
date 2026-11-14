package com.cleanlearning.domain.consultation.outcome;

import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import java.util.List;
import java.util.Objects;

public record CaseCancellation(
    ConsultationCaseId id,
    CancellationOutcome outcome,
    List<AppointmentCancellation> appointments) {

  public CaseCancellation {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(outcome, "outcome");
    appointments = List.copyOf(appointments);
  }
}
