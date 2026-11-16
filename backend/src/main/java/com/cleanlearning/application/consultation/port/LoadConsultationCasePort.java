package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LoadConsultationCasePort {

  /** Without a lock: the eligible experts and the slots only show what a booking would check. */
  Optional<ConsultationCase> load(ConsultationCaseId id, UserId customerId);
}
