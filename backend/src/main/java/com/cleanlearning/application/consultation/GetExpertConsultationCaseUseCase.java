package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadExpertConsultationCasePort;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.view.ExpertConsultationCase;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A case of the current user as its assigned expert (UC-11 step 4); any other case is a 404. */
@Service
public class GetExpertConsultationCaseUseCase {

  private final LoadExpertConsultationCasePort loadCase;

  private final Clock clock;

  GetExpertConsultationCaseUseCase(LoadExpertConsultationCasePort loadCase, Clock clock) {
    this.loadCase = loadCase;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public ExpertConsultationCase invoke(CurrentUser user, ConsultationCaseId id) {
    return loadCase
        .load(id, user.id(), clock.instant())
        .orElseThrow(() -> new NotFoundException("Case " + id.value() + " not found."));
  }
}
