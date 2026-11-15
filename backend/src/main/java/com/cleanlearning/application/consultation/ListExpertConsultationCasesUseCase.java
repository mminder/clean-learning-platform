package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadExpertConsultationCasesPort;
import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.view.ExpertConsultationCaseSummary;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The current user's cases as their assigned expert, in the order of "My purchases" (D-17). */
@Service
public class ListExpertConsultationCasesUseCase {

  private final LoadExpertConsultationCasesPort loadCases;

  private final Clock clock;

  ListExpertConsultationCasesUseCase(LoadExpertConsultationCasesPort loadCases, Clock clock) {
    this.loadCases = loadCases;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<ExpertConsultationCaseSummary> invoke(CurrentUser user) {
    Instant now = clock.instant();
    return loadCases.loadAll(user.id(), now).stream()
        .sorted(
            Comparator.comparing(ExpertConsultationCaseSummary::inList, CaseInList.orderAt(now)))
        .toList();
  }
}
