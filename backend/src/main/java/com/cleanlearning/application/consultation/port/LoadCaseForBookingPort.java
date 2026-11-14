package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LoadCaseForBookingPort {

  /** Without a lock: the eligible experts and the slots only show what a booking would check. */
  Optional<CaseForBooking> load(ConsultationCaseId id, UserId customerId);
}
