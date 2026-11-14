package com.cleanlearning.application.consultation;

import com.cleanlearning.application.availability.port.LoadAvailabilityPort;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadCustomerBusyTimesPort;
import com.cleanlearning.application.consultation.port.LockCaseForBookingPort;
import com.cleanlearning.application.consultation.port.SaveBookingPort;
import com.cleanlearning.application.user.port.LockExpertPort;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.SlotFinder;
import com.cleanlearning.domain.consultation.outcome.Booking;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.consultation.view.CustomerAppointment;
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

/**
 * Takes two row locks (D-27): the case, then the expert, so every rule is checked again on what's
 * current. The offering isn't locked: eligibility is read when the booking runs. Nor is the
 * customer: their other appointments are read, not locked, so two bookings by one customer at the
 * same instant, on two cases with two experts, could still overlap (accepted).
 */
@Service
public class BookConsultationAppointmentUseCase {

  private final LockCaseForBookingPort lockCase;

  private final LockExpertPort lockExpert;

  private final LoadAvailabilityPort loadAvailability;

  private final LoadBusyTimesPort loadBusyTimes;

  private final LoadCustomerBusyTimesPort loadCustomerBusyTimes;

  private final SaveBookingPort saveBooking;

  private final Clock clock;

  BookConsultationAppointmentUseCase(
      LockCaseForBookingPort lockCase,
      LockExpertPort lockExpert,
      LoadAvailabilityPort loadAvailability,
      LoadBusyTimesPort loadBusyTimes,
      LoadCustomerBusyTimesPort loadCustomerBusyTimes,
      SaveBookingPort saveBooking,
      Clock clock) {
    this.lockCase = lockCase;
    this.lockExpert = lockExpert;
    this.loadAvailability = loadAvailability;
    this.loadBusyTimes = loadBusyTimes;
    this.loadCustomerBusyTimes = loadCustomerBusyTimes;
    this.saveBooking = saveBooking;
    this.clock = clock;
  }

  @Transactional
  public CustomerAppointment invoke(CurrentUser user, BookConsultationAppointmentCommand command) {
    CaseForBooking consultationCase =
        lockCase
            .lock(command.consultationCaseId(), user.id())
            .orElseThrow(() ->
                new NotFoundException(
                    "Case " + command.consultationCaseId().value() + " not found."));
    // A cancelled case first, then the credits, so a booking on a case cancelled in another tab is
    // CASE_CANCELLED, and a second booking on the last credit NO_OPEN_CREDITS, whichever expert it
    // names.
    consultationCase.requireActive();
    consultationCase.requireOpenCredit();
    UserId expertId = consultationCase.expertFor(command.expertId());
    // The expert after the case (D-27).
    lockExpert.lock(expertId);
    // The time after waiting for the locks. The customer's own appointments are busy time too.
    Instant now = clock.instant();
    LocalDate today = LocalDate.ofInstant(now, BusinessTimeZone.ZONE);
    List<BusyTime> busyTimes =
        Stream.concat(
                loadBusyTimes.load(expertId, now).stream(),
                loadCustomerBusyTimes.loadCustomerBusyTimes(user.id(), now).stream())
            .toList();
    SlotFinder slotFinder = new SlotFinder(loadAvailability.load(expertId, today), busyTimes);
    Booking booking = consultationCase.book(expertId, command.start(), slotFinder, now);
    saveBooking.save(booking);
    return CustomerAppointment.of(booking.appointment(), now);
  }
}
