package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.view.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;

public interface LoadCustomerConsultationCasesPort {

  /**
   * The adapter only passes {@code asOf} into the domain records, which derive the states from it
   * (D-11).
   */
  List<CustomerConsultationCaseSummary> loadAll(UserId customerId, Instant asOf);
}
