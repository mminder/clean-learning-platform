package com.cleanlearning.application.availability.port;

import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.user.value.UserId;
import java.time.LocalDate;

public interface LoadAvailabilityPort {

  Availability load(UserId expertId, LocalDate from);
}
