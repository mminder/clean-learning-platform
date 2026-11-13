package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Instant;
import java.util.List;

public interface LoadBusyTimesPort {

  /**
   * The expert's busy time that ends after {@code from}, in any order: the {@code BOOKED}
   * appointments of every case the expert is assigned to, whoever its customer is (UC-06). Runs add
   * their time slots with the trainings (spec 04, decision 14).
   */
  List<BusyTime> load(UserId expertId, Instant from);
}
