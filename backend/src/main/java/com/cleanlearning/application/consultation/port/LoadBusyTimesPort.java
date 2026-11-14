package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;

public interface LoadBusyTimesPort {

  /**
   * The {@code BOOKED} appointments of every case the expert is assigned to, whoever its customer
   * is.
   */
  List<BusyTime> load(UserId expertId, Instant from);
}
