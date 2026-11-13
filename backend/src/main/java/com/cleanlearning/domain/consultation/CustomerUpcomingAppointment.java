package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.Objects;

import com.cleanlearning.domain.offering.value.CancellationDeadline;

/**
 * An upcoming appointment as its customer sees it on the case detail at a given time (UC-05, UC-07): the appointment
 * with its status, and its cancellation preview, which the cancel dialog reads (spec 05, decision 12). The server
 * computes all of it; the frontend never compares a time with a deadline.
 *
 * @param canCancel whether it can be cancelled: it hasn't started
 * @param freeCancellationUntil until when cancelling it is free, for the late warning
 * @param creditRefundedIfCancelledNow whether cancelling it now would refund its credit
 */
public record CustomerUpcomingAppointment(CustomerAppointment appointment, boolean canCancel,
        Instant freeCancellationUntil, boolean creditRefundedIfCancelledNow) {

    public CustomerUpcomingAppointment {
        Objects.requireNonNull(appointment, "appointment");
        Objects.requireNonNull(freeCancellationUntil, "freeCancellationUntil");
    }

    /** The appointment at {@code asOf}, with its preview from {@link Appointment}'s rules and the case's deadline. */
    public static CustomerUpcomingAppointment of(Appointment appointment, CancellationDeadline deadline,
            Instant asOf) {
        return new CustomerUpcomingAppointment(appointment.at(asOf), appointment.isCancellableAt(asOf),
                appointment.freeCancellationUntil(deadline),
                appointment.isCreditRefundedIfCancelledAt(deadline, asOf));
    }
}
