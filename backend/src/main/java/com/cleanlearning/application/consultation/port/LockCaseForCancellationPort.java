package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.CaseForCancellation;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LockCaseForCancellationPort {

  /**
   * The case is the only lock of a cancellation (D-27), so a cancellation and a booking of one
   * case, or two cancellations, run one after the other.
   */
  Optional<CaseForCancellation> lock(ConsultationCaseId id, UserId customerId);
}
