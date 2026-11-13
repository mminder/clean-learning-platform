package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.CancellationOutcome;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import java.util.List;
import java.util.Objects;

/**
 * What cancelling a case writes (UC-08 step 4): the case, now {@code CANCELLED} with its outcome,
 * and the cancellation of each of its appointments that hadn't started, each with its credit
 * refunded before its free-cancellation time and forfeited after (spec 05, decision 7).
 *
 * @param appointments the appointments cancelled with the case
 */
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
