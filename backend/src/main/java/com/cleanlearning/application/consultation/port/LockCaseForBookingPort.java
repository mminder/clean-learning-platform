package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LockCaseForBookingPort {

  /**
   * Called first by the booking (D-27), so two bookings of one case run one after the other, and
   * the second sees the first's appointment and expert.
   */
  Optional<CaseForBooking> lock(ConsultationCaseId id, UserId customerId);
}
