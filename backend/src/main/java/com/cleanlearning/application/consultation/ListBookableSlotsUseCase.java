package com.cleanlearning.application.consultation;

import com.cleanlearning.application.availability.port.LoadAvailabilityPort;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadConsultationCasePort;
import com.cleanlearning.application.consultation.port.LoadCustomerBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadEligibleExpertsPort;
import com.cleanlearning.domain.consultation.ConsultationCase;
import com.cleanlearning.domain.consultation.SlotFinder;
import com.cleanlearning.domain.consultation.value.BookableSlot;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.value.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Takes no lock: it only shows, and the booking checks again under its locks. */
@Service
public class ListBookableSlotsUseCase {

  private final LoadConsultationCasePort loadCase;

  private final LoadEligibleExpertsPort loadEligibleExperts;

  private final LoadAvailabilityPort loadAvailability;

  private final LoadBusyTimesPort loadBusyTimes;

  private final LoadCustomerBusyTimesPort loadCustomerBusyTimes;

  private final Clock clock;

  ListBookableSlotsUseCase(
      LoadConsultationCasePort loadCase,
      LoadEligibleExpertsPort loadEligibleExperts,
      LoadAvailabilityPort loadAvailability,
      LoadBusyTimesPort loadBusyTimes,
      LoadCustomerBusyTimesPort loadCustomerBusyTimes,
      Clock clock) {
    this.loadCase = loadCase;
    this.loadEligibleExperts = loadEligibleExperts;
    this.loadAvailability = loadAvailability;
    this.loadBusyTimes = loadBusyTimes;
    this.loadCustomerBusyTimes = loadCustomerBusyTimes;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<BookableSlot> invoke(CurrentUser user, ConsultationCaseId id, UserId expertId) {
    ConsultationCase consultationCase =
        loadCase
            .load(id, user.id())
            .orElseThrow(() -> new NotFoundException("Case " + id.value() + " not found."));
    consultationCase.requireActive();
    UserId expert =
        consultationCase.expertFor(
            expertId, loadEligibleExperts.load(consultationCase.offeringId()));
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, BusinessTimeZone.ZONE);
    List<BusyTime> busyTimes =
        Stream.concat(
                loadBusyTimes.load(expert, now).stream(),
                loadCustomerBusyTimes.loadCustomerBusyTimes(user.id(), now).stream())
            .toList();
    SlotFinder slotFinder = new SlotFinder(loadAvailability.load(expert, today), busyTimes);
    return consultationCase.bookableSlots(slotFinder, now);
  }
}
