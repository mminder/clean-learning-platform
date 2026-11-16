package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LockConsultationCasePort {

  /**
   * The case is the first lock of a booking and the only lock of a cancellation (D-27), so two
   * bookings of one case, a cancellation and a booking, or two cancellations run one after the
   * other, and the second sees what the first did: its appointment and expert, or the cancellation.
   */
  Optional<ConsultationCase> lock(ConsultationCaseId id, UserId customerId);
}
