package com.cleanlearning.application.availability.port;

import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.user.value.UserId;
import java.time.LocalDate;

public interface ReplaceAvailabilityPort {

  void replaceFrom(UserId expertId, LocalDate from, Availability availability);
}
