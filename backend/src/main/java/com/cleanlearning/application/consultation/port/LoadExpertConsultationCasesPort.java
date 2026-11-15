package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.view.ExpertConsultationCaseSummary;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;

public interface LoadExpertConsultationCasesPort {

  /**
   * The cases whose assigned expert is {@code expertId}. The adapter only passes {@code asOf} into
   * the domain records, which derive the states from it (D-11).
   */
  List<ExpertConsultationCaseSummary> loadAll(UserId expertId, Instant asOf);
}
