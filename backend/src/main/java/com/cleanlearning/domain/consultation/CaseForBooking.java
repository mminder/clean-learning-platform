package com.cleanlearning.domain.consultation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.BookableSlot;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.offering.EligibleExpert;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.user.value.UserId;

/**
 * A case as a booking needs it (UC-06): its purchase terms, its assigned expert, if any, the number of its booked
 * appointments, and its offering's current eligible experts, by display name. It holds UC-06's rules: a booking needs
 * an open credit, is with the case's expert or, on the first booking, with an eligible one, and starts at a bookable
 * slot. {@link #book} checks every rule itself, credits first.
 *
 * @param assignedExpertId the case's expert, empty until the first booking
 * @param bookedAppointments the case's {@code BOOKED} appointments, consumed or not
 */
public record CaseForBooking(ConsultationCaseId id, PurchaseTerms purchaseTerms, Optional<UserId> assignedExpertId,
        int bookedAppointments, List<EligibleExpert> eligibleExperts) {

    /** Every credit of the case is booked. */
    public static final String NO_OPEN_CREDITS = "NO_OPEN_CREDITS";

    /** The booking names another expert than the case's, which a first booking in another tab assigned. */
    public static final String EXPERT_CHANGED = "EXPERT_CHANGED";

    /** On a first booking, the named expert isn't (or is no longer) eligible for the case's offering. */
    public static final String EXPERT_NOT_ELIGIBLE = "EXPERT_NOT_ELIGIBLE";

    /** The start isn't a bookable slot of the expert, for example because someone else booked it meanwhile. */
    public static final String SLOT_NOT_AVAILABLE = "SLOT_NOT_AVAILABLE";

    public CaseForBooking {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(purchaseTerms, "purchaseTerms");
        Objects.requireNonNull(assignedExpertId, "assignedExpertId");
        if (bookedAppointments < 0) {
            throw new IllegalArgumentException(
                    "A case has no negative number of booked appointments: " + bookedAppointments);
        }
        eligibleExperts = List.copyOf(eligibleExperts);
    }

    /** The session count less the booked appointments, counted as the case detail counts them. */
    public int openCredits() {
        return Appointment.openCredits(purchaseTerms.sessionCount(), bookedAppointments);
    }

    /** @throws BusinessRuleException {@link #NO_OPEN_CREDITS} if every credit is booked */
    public void requireOpenCredit() {
        if (openCredits() <= 0) {
            throw new BusinessRuleException(NO_OPEN_CREDITS, "This case has no open credits.");
        }
    }

    /**
     * The expert a booking naming {@code named} is with (spec 04, decision 12): the case's expert, who stays after they
     * stop being eligible, or on the first booking the named one, if they're eligible.
     *
     * @throws BusinessRuleException {@link #EXPERT_CHANGED} if the case has another expert, or
     *     {@link #EXPERT_NOT_ELIGIBLE} if it has none and the named one isn't eligible
     */
    public UserId expertFor(UserId named) {
        if (assignedExpertId.isPresent()) {
            if (!assignedExpertId.get().equals(named)) {
                throw new BusinessRuleException(EXPERT_CHANGED,
                        "This case already has an expert. Please choose a time with them.");
            }
            return named;
        }
        if (eligibleExperts.stream().noneMatch(expert -> expert.id().equals(named))) {
            throw new BusinessRuleException(EXPERT_NOT_ELIGIBLE,
                    "This expert is no longer available for this offering. Please choose another.");
        }
        return named;
    }

    /**
     * The slots the customer can book with the expert whose availability and busy time {@code slotFinder} holds,
     * soonest first: each start time for the case's session duration, with its end and its free-cancellation time from
     * the case's cancellation deadline (UC-06 steps 3–5).
     */
    public List<BookableSlot> bookableSlots(SlotFinder slotFinder, Instant now) {
        return slotFinder.startTimes(purchaseTerms.sessionDuration(), now).stream()
                .map(start -> new BookableSlot(start, endOf(start),
                        purchaseTerms.cancellationDeadline().freeUntil(start)))
                .toList();
    }

    /**
     * Books a session at {@code start} with the named expert (UC-06 step 7): a new appointment, and on the first
     * booking the expert it assigns. It checks every rule itself, in this order: an open credit, the expert, the slot.
     *
     * @param slotFinder the named expert's availability and busy time
     * @throws BusinessRuleException {@link #NO_OPEN_CREDITS}, {@link #EXPERT_CHANGED}, {@link #EXPERT_NOT_ELIGIBLE},
     *     or {@link #SLOT_NOT_AVAILABLE} if {@code start} isn't a bookable slot
     */
    public Booking book(UserId expertId, Instant start, SlotFinder slotFinder, Instant now) {
        requireOpenCredit();
        UserId expert = expertFor(expertId);
        if (!slotFinder.isBookable(start, purchaseTerms.sessionDuration(), now)) {
            throw new BusinessRuleException(SLOT_NOT_AVAILABLE, "This time is no longer available.");
        }
        Appointment appointment = new Appointment(new AppointmentId(UUID.randomUUID()), id, start, endOf(start));
        return new Booking(appointment, expert, assignedExpertId.isEmpty());
    }

    private Instant endOf(Instant start) {
        return start.plus(Duration.ofMinutes(purchaseTerms.sessionDuration().minutes()));
    }
}
