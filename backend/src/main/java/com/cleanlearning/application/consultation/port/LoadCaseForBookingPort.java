package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LoadCaseForBookingPort {

  /**
   * The customer's case as a booking needs it, whatever its offering's status, without a lock, or
   * empty if there's none with this id or it's another customer's: the query is scoped to the
   * customer, so both look the same (security.md). The eligible experts and the slots show what a
   * booking would check.
   */
  Optional<CaseForBooking> load(ConsultationCaseId id, UserId customerId);
}
