package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;

public interface LoadCustomerBusyTimesPort {

  /**
   * The {@code BOOKED} appointments of all the customer's cases, whoever the expert is: a
   * customer's appointments never overlap.
   */
  List<BusyTime> loadCustomerBusyTimes(UserId customerId, Instant from);
}
