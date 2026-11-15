package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;

public interface CheckAssignedExpertPort {

  /** Whether {@code expertId} is the case's assigned expert: false for an unknown case, too. */
  boolean isAssignedExpert(ConsultationCaseId id, UserId expertId);
}
