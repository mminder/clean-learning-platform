package com.cleanlearning.application.availability;

import com.cleanlearning.application.availability.port.ReplaceAvailabilityPort;
import com.cleanlearning.application.user.port.LockExpertPort;
import com.cleanlearning.domain.availability.Availability;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.time.LocalDate;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaveAvailabilityUseCase {

  private final LockExpertPort lockExpert;

  private final ReplaceAvailabilityPort replaceAvailability;

  private final Clock clock;

  SaveAvailabilityUseCase(
      LockExpertPort lockExpert, ReplaceAvailabilityPort replaceAvailability, Clock clock) {
    this.lockExpert = lockExpert;
    this.replaceAvailability = replaceAvailability;
    this.clock = clock;
  }

  /**
   * Locks the expert first (D-27), so two saves, or a save and a booking, of one expert run one
   * after the other. Dates before today stay.
   */
  @Transactional
  public Availability invoke(CurrentUser user, SaveAvailabilityCommand command) {
    lockExpert.lock(user.id());
    LocalDate today = LocalDate.ofInstant(clock.instant(), BusinessTimeZone.ZONE);
    Availability availability =
        new Availability(new TreeSet<>(command.dates()), command.timeRanges());
    availability.requireNoDateBefore(today);
    replaceAvailability.replaceFrom(user.id(), today, availability);
    return availability;
  }
}
