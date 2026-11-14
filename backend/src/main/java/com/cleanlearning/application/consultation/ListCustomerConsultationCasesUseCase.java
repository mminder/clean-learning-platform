package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasesPort;
import com.cleanlearning.domain.consultation.CaseInList;
import com.cleanlearning.domain.consultation.view.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCustomerConsultationCasesUseCase {

  private final LoadCustomerConsultationCasesPort loadCases;

  private final Clock clock;

  ListCustomerConsultationCasesUseCase(LoadCustomerConsultationCasesPort loadCases, Clock clock) {
    this.loadCases = loadCases;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<CustomerConsultationCaseSummary> invoke(CurrentUser user) {
    Instant now = clock.instant();
    return loadCases.loadAll(user.id(), now).stream()
        .sorted(
            Comparator.comparing(CustomerConsultationCaseSummary::inList, CaseInList.orderAt(now)))
        .toList();
  }
}
