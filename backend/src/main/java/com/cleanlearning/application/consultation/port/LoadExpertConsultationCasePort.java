package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.view.ExpertConsultationCase;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.Optional;

public interface LoadExpertConsultationCasePort {

  /**
   * The case only if {@code expertId} is its assigned expert: another expert's case is empty, like
   * an unknown id. The adapter only passes {@code asOf} into the domain record (D-11).
   */
  Optional<ExpertConsultationCase> load(ConsultationCaseId id, UserId expertId, Instant asOf);
}
