package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.read.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;

public interface LoadCustomerConsultationCasesPort {

  /**
   * The customer's own cases at {@code asOf}, whatever their offerings' status, in no particular
   * order, each with its assigned expert's name and its booked appointments. The queries are scoped
   * to the customer, so another customer's case or appointment never shows (security.md). The
   * adapter only passes {@code asOf} into the domain records, which derive the states from it
   * (D-11).
   */
  List<CustomerConsultationCaseSummary> loadAll(UserId customerId, Instant asOf);
}
