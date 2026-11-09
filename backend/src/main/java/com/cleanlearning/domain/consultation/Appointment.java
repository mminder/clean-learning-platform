package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.cleanlearning.domain.consultation.value.AppointmentId;
import com.cleanlearning.domain.consultation.value.AppointmentStatus;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.value.SessionCount;

/**
 * A booked appointment of a case (UC-06): one session with the case's assigned expert, from start to end. It holds the
 * appointments' rules at a given time, which the case detail and "My purchases" share (glossary, spec 04 decision 21):
 * it's {@code BOOKED} until its start and {@code CONSUMED} from its start on, and upcoming until its end, so a running
 * appointment is consumed and upcoming at once; and the open credits, which the booking checks, too. The column's
 * check repeats that it ends after it starts (D-36).
 */
public record Appointment(AppointmentId id, ConsultationCaseId consultationCaseId, Instant start, Instant end) {

    /**
     * Soonest first; two appointments of a case never start at once, and the id, as text, makes the order total
     * anyway, as in {@link CustomerConsultationCaseSummary#MY_PURCHASES_ORDER}.
     */
    private static final Comparator<Appointment> BY_START = Comparator.comparing(Appointment::start)
            .thenComparing(appointment -> appointment.id().value().toString());

    public Appointment {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(consultationCaseId, "consultationCaseId");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("An appointment ends after it starts: " + start + " to " + end + ".");
        }
    }

    /**
     * The upcoming appointments at {@code asOf}, the ones that haven't ended, soonest first, each with its status: a
     * running one is {@code CONSUMED}.
     */
    public static List<CustomerAppointment> upcoming(List<Appointment> appointments, Instant asOf) {
        return appointments.stream().filter(appointment -> appointment.isUpcomingAt(asOf)).sorted(BY_START)
                .map(appointment -> appointment.at(asOf)).toList();
    }

    /** The past appointments at {@code asOf}, the ones that have ended, newest first, all {@code CONSUMED}. */
    public static List<CustomerAppointment> past(List<Appointment> appointments, Instant asOf) {
        return appointments.stream().filter(appointment -> !appointment.isUpcomingAt(asOf))
                .sorted(BY_START.reversed()).map(appointment -> appointment.at(asOf)).toList();
    }

    /**
     * Computed, never stored (D-11): the open credits of a case with this session count and this many booked
     * appointments, each of which uses a credit, consumed or not, until forfeited credits (UC-07) and credit
     * adjustments (UC-18) exist (spec 04, decision 15). The booking's check, the case detail and "My purchases" all
     * count with this one rule, so what the pages show and what the booking allows can't drift apart.
     */
    public static int openCredits(SessionCount sessionCount, int bookedAppointments) {
        return sessionCount.value() - bookedAppointments;
    }

    /** Computed, never stored (D-11): {@code BOOKED} while the start is ahead, {@code CONSUMED} from the start on. */
    public AppointmentStatus statusAt(Instant asOf) {
        return start.isAfter(asOf) ? AppointmentStatus.BOOKED : AppointmentStatus.CONSUMED;
    }

    /** Upcoming until it ends, also while it runs (spec 04, decision 21). */
    public boolean isUpcomingAt(Instant asOf) {
        return end.isAfter(asOf);
    }

    /** The appointment as its customer sees it at {@code asOf}. */
    public CustomerAppointment at(Instant asOf) {
        return new CustomerAppointment(id, start, end, statusAt(asOf));
    }
}
