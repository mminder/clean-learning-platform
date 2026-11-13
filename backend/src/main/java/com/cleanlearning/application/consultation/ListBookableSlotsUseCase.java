package com.cleanlearning.application.consultation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.availability.port.LoadAvailabilityPort;
import com.cleanlearning.application.consultation.port.LoadBusyTimesPort;
import com.cleanlearning.application.consultation.port.LoadCaseForBookingPort;
import com.cleanlearning.application.consultation.port.LoadCustomerBusyTimesPort;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.SlotFinder;
import com.cleanlearning.domain.consultation.value.BookableSlot;
import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.BusinessTimeZone;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.value.UserId;

/**
 * UC-06 steps 3–4: the customer sees the times they can book with the expert, all from now on (spec 04, decision 5).
 * It takes no lock: it only shows, and the booking checks again under its locks.
 */
@Service
public class ListBookableSlotsUseCase {

    private final LoadCaseForBookingPort loadCase;

    private final LoadAvailabilityPort loadAvailability;

    private final LoadBusyTimesPort loadBusyTimes;

    private final LoadCustomerBusyTimesPort loadCustomerBusyTimes;

    private final Clock clock;

    ListBookableSlotsUseCase(LoadCaseForBookingPort loadCase, LoadAvailabilityPort loadAvailability,
            LoadBusyTimesPort loadBusyTimes, LoadCustomerBusyTimesPort loadCustomerBusyTimes, Clock clock) {
        this.loadCase = loadCase;
        this.loadAvailability = loadAvailability;
        this.loadBusyTimes = loadBusyTimes;
        this.loadCustomerBusyTimes = loadCustomerBusyTimes;
        this.clock = clock;
    }

    /**
     * The case's bookable slots with the expert, soonest first, each with its end and its free-cancellation time, if
     * that's still ahead. A slot overlaps neither the expert's busy time nor the customer's own appointments, of any
     * case (spec 04, decision 22).
     *
     * @param expertId the expert the dialog shows: the one the customer picked, or the case's
     * @throws NotFoundException if there's no case with this id, or it's another customer's
     * @throws BusinessRuleException if the case was cancelled ({@code CASE_CANCELLED}), has another expert
     *     ({@code EXPERT_CHANGED}), or has none and the expert isn't eligible ({@code EXPERT_NOT_ELIGIBLE})
     */
    @Transactional(readOnly = true)
    public List<BookableSlot> invoke(CurrentUser user, ConsultationCaseId id, UserId expertId) {
        CaseForBooking consultationCase = loadCase.load(id, user.id())
                .orElseThrow(() -> new NotFoundException("Case " + id.value() + " not found."));
        consultationCase.requireActive();
        UserId expert = consultationCase.expertFor(expertId);
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, BusinessTimeZone.ZONE);
        List<BusyTime> busyTimes = Stream.concat(loadBusyTimes.load(expert, now).stream(),
                loadCustomerBusyTimes.loadCustomerBusyTimes(user.id(), now).stream()).toList();
        SlotFinder slotFinder = new SlotFinder(loadAvailability.load(expert, today), busyTimes);
        return consultationCase.bookableSlots(slotFinder, now);
    }
}
