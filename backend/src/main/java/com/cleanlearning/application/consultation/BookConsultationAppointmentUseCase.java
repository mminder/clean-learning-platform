package com.cleanlearning.application.consultation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.availability.port.LoadAvailabilityPort;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LockCaseForBookingPort;
import com.cleanlearning.application.consultation.port.SaveBookingPort;
import com.cleanlearning.application.user.port.LockExpertPort;
import com.cleanlearning.domain.consultation.Booking;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.CustomerAppointment;
import com.cleanlearning.domain.consultation.SlotFinder;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.value.UserId;

/**
 * UC-06 steps 6–7: the customer books a session on their case. It takes two row locks, in the order of D-27: the case,
 * then the expert. Under both, nobody else books on this case or with this expert, and the expert's availability
 * doesn't change, so every rule is checked again on what's current. The offering isn't locked: eligibility is read
 * when the booking runs (UC-06).
 */
@Service
public class BookConsultationAppointmentUseCase {

    private final LockCaseForBookingPort lockCase;

    private final LockExpertPort lockExpert;

    private final LoadAvailabilityPort loadAvailability;

    private final LoadBusyTimesPort loadBusyTimes;

    private final SaveBookingPort saveBooking;

    private final Clock clock;

    BookConsultationAppointmentUseCase(LockCaseForBookingPort lockCase, LockExpertPort lockExpert,
            LoadAvailabilityPort loadAvailability, LoadBusyTimesPort loadBusyTimes, SaveBookingPort saveBooking,
            Clock clock) {
        this.lockCase = lockCase;
        this.lockExpert = lockExpert;
        this.loadAvailability = loadAvailability;
        this.loadBusyTimes = loadBusyTimes;
        this.saveBooking = saveBooking;
        this.clock = clock;
    }

    /**
     * Locks the case, chooses the expert to lock, locks the expert, loads their availability and busy time, lets the
     * domain decide, and writes the appointment, with the case's expert on the first booking. Answers the new
     * appointment as its customer sees it: {@code BOOKED}.
     *
     * @throws NotFoundException if there's no case with this id, or it's another customer's
     * @throws BusinessRuleException {@code NO_OPEN_CREDITS}, {@code EXPERT_CHANGED}, {@code EXPERT_NOT_ELIGIBLE} or
     *     {@code SLOT_NOT_AVAILABLE}, as {@code CaseForBooking} decides
     */
    @Transactional
    public CustomerAppointment invoke(CurrentUser user, BookConsultationAppointmentCommand command) {
        // 1. The case first (D-27), scoped to the customer: a second booking of it waits here.
        CaseForBooking consultationCase = lockCase.lock(command.consultationCaseId(), user.id())
                .orElseThrow(() -> new NotFoundException(
                        "Case " + command.consultationCaseId().value() + " not found."));
        // 2. The expert to lock: credits first, so a second booking on the last credit is NO_OPEN_CREDITS whichever
        // expert it names.
        consultationCase.requireOpenCredit();
        UserId expertId = consultationCase.expertFor(command.expertId());
        // 3. The expert after the case (D-27): a booking of another case with this expert, or a save of their
        // availability, waits here.
        lockExpert.lock(expertId);
        // 4. Under both locks, what's current; the time after waiting for them.
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, BusinessTimeZone.ZONE);
        SlotFinder slotFinder = new SlotFinder(loadAvailability.load(expertId, today),
                loadBusyTimes.load(expertId, now));
        // 5. The domain decides, checking every rule again.
        Booking booking = consultationCase.book(expertId, command.start(), slotFinder, now);
        // 6. Write.
        saveBooking.save(booking);
        // 7. Answer.
        return booking.appointment().at(now);
    }
}
