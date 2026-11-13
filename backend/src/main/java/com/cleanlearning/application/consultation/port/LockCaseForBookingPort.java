package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LockCaseForBookingPort {

  /**
   * Locks the customer's case row until the transaction ends, waiting for every other lock of the
   * row, and then loads the case as a booking needs it, whatever its offering's status; or returns
   * empty, locking nothing, if there's no case with this id or it's another customer's. The query
   * is scoped to the customer, so only the customer's own case is ever locked (security.md). The
   * booking calls this first (D-27: offering → case → expert → run), so two bookings of one case
   * run one after the other, and the second sees the first's appointment and expert.
   */
  Optional<CaseForBooking> lock(ConsultationCaseId id, UserId customerId);
}
