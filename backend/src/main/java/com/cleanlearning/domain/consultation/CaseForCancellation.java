package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.shared.BusinessRuleException;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * A case as a cancellation needs it (UC-07): its purchase terms, which hold the cancellation deadline, its booked
 * appointments, consumed or not, and its cancelled ones. It holds UC-07's rules; {@link #cancelAppointment} checks
 * every one itself, in the plan's order.
 *
 * @param bookedAppointments the case's {@code BOOKED} appointments
 * @param cancelledAppointments the case's {@code CANCELLED} appointments, each with whether its credit was refunded
 */
public record CaseForCancellation(ConsultationCaseId id, PurchaseTerms purchaseTerms,
        List<Appointment> bookedAppointments, List<AppointmentCancellation> cancelledAppointments) {

    /** The appointment was cancelled before, in another tab or with its case. */
    public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";

    /** The appointment has started: it's consumed and can no longer be cancelled. */
    public static final String APPOINTMENT_STARTED = "APPOINTMENT_STARTED";

    /**
     * Whether the credit is refunded now isn't what the customer confirmed: from the dialog, the free cancellation
     * deadline passed while it was open (spec 05, decision 4).
     */
    public static final String DEADLINE_PASSED = "DEADLINE_PASSED";

    public CaseForCancellation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(purchaseTerms, "purchaseTerms");
        bookedAppointments = List.copyOf(bookedAppointments);
        cancelledAppointments = List.copyOf(cancelledAppointments);
        Set<AppointmentId> booked = bookedAppointments.stream().map(Appointment::id).collect(Collectors.toSet());
        if (cancelledAppointments.stream().anyMatch(cancelled -> booked.contains(cancelled.appointmentId()))) {
            throw new IllegalArgumentException("An appointment is either booked or cancelled, never both.");
        }
    }

    /** The credits of the appointments cancelled late, which the open credits lack (glossary). */
    public int forfeitedCredits() {
        return Math.toIntExact(cancelledAppointments.stream().filter(cancelled -> !cancelled.creditRefunded()).count());
    }

    /**
     * Cancels the case's appointment (UC-07 step 4), with its credit refunded if {@code now} is before its free
     * cancellation time, and only with the outcome the customer confirmed (spec 05, decision 4). It checks every rule
     * itself, in this order: the case's appointment, not cancelled yet, not started, the outcome.
     *
     * @param creditRefundedConfirmed whether the dialog said the credit would be returned
     * @throws NotFoundException if the appointment is none of the case's, booked or cancelled
     * @throws BusinessRuleException {@link #APPOINTMENT_CANCELLED}, {@link #APPOINTMENT_STARTED}, or
     *     {@link #DEADLINE_PASSED} if whether the credit is refunded at {@code now} isn't what was confirmed
     */
    public AppointmentCancellation cancelAppointment(AppointmentId appointmentId, boolean creditRefundedConfirmed,
            Instant now) {
        Optional<Appointment> booked = bookedAppointments.stream()
                .filter(appointment -> appointment.id().equals(appointmentId)).findFirst();
        if (booked.isEmpty()) {
            if (cancelledAppointments.stream().anyMatch(cancelled -> cancelled.appointmentId().equals(appointmentId))) {
                throw new BusinessRuleException(APPOINTMENT_CANCELLED, "This appointment was already cancelled.");
            }
            throw new NotFoundException("Appointment " + appointmentId.value() + " not found.");
        }
        Appointment appointment = booked.get();
        if (!appointment.isCancellableAt(now)) {
            throw new BusinessRuleException(APPOINTMENT_STARTED, "This appointment can no longer be cancelled.");
        }
        boolean creditRefunded = appointment.isCreditRefundedIfCancelledAt(purchaseTerms.cancellationDeadline(), now);
        if (creditRefunded != creditRefundedConfirmed) {
            throw new BusinessRuleException(DEADLINE_PASSED,
                    "The free cancellation deadline has just passed. Please confirm again.");
        }
        return new AppointmentCancellation(appointmentId, creditRefunded);
    }
}
