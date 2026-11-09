package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseGroup;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.user.value.DisplayName;

/**
 * A case as its customer sees it on "My purchases" at {@code asOf} (UC-05 step 2): the offering's name and image, read
 * live (D-08), the assigned expert's name, the purchase time, the credits, the next appointment and the group. What
 * depends on the time is derived at {@code asOf}, never stored (D-11); {@link Appointment} holds the appointments'
 * rules, which the case detail uses, too. The session count is the purchase terms' one, which the credits come from.
 *
 * @param expertDisplayName the assigned expert's name, empty until the first booking
 * @param appointments the case's {@code BOOKED} appointments, in any order
 * @param asOf the time the case is shown at, from the {@code Clock}
 */
public record CustomerConsultationCaseSummary(ConsultationCaseId id, OfferingName offeringName,
        Optional<ImageId> offeringImageId, SessionCount sessionCount, Instant purchasedAt,
        Optional<DisplayName> expertDisplayName, List<Appointment> appointments, Instant asOf) {

    /**
     * The order of "My purchases": by group, then Upcoming by the next appointment, soonest first, Needs booking by
     * purchase time, oldest first, and Past by the last appointment, newest first; then by id (D-17), as text: that's
     * Postgres's order of a uuid, which {@code UUID.compareTo} (signed) isn't.
     */
    public static final Comparator<CustomerConsultationCaseSummary> MY_PURCHASES_ORDER = Comparator
            .comparing(CustomerConsultationCaseSummary::group)
            .thenComparing(CustomerConsultationCaseSummary::compareWithinGroup)
            .thenComparing(summary -> summary.id().value().toString());

    public CustomerConsultationCaseSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(offeringName, "offeringName");
        Objects.requireNonNull(offeringImageId, "offeringImageId");
        Objects.requireNonNull(sessionCount, "sessionCount");
        Objects.requireNonNull(purchasedAt, "purchasedAt");
        Objects.requireNonNull(expertDisplayName, "expertDisplayName");
        appointments = List.copyOf(appointments);
        Objects.requireNonNull(asOf, "asOf");
    }

    /** The purchase terms' session count, until credit adjustments exist (UC-18). */
    public int totalCredits() {
        return sessionCount.value();
    }

    /** Every booked appointment uses a credit, consumed or not, counted as the booking counts them. */
    public int openCredits() {
        return Appointment.openCredits(sessionCount, appointments.size());
    }

    /**
     * The soonest appointment that hasn't ended, {@code CONSUMED} while it runs (spec 04, decision 21); empty without
     * one.
     */
    public Optional<CustomerAppointment> nextAppointment() {
        return Appointment.upcoming(appointments, asOf).stream().findFirst();
    }

    /**
     * Computed, never stored (D-11): {@code UPCOMING} with an appointment that hasn't ended, otherwise
     * {@code NEEDS_BOOKING} with an open credit, otherwise {@code PAST}. UC-08 adds "active" to the second and
     * cancelled cases to the third.
     */
    public PurchaseGroup group() {
        if (nextAppointment().isPresent()) {
            return PurchaseGroup.UPCOMING;
        }
        return openCredits() > 0 ? PurchaseGroup.NEEDS_BOOKING : PurchaseGroup.PAST;
    }

    /** Compares two cases of the same group. A past case has an appointment: its credits are all booked. */
    private static int compareWithinGroup(CustomerConsultationCaseSummary first,
            CustomerConsultationCaseSummary second) {
        return switch (first.group()) {
            case UPCOMING -> first.nextAppointment().orElseThrow().start()
                    .compareTo(second.nextAppointment().orElseThrow().start());
            case NEEDS_BOOKING -> first.purchasedAt().compareTo(second.purchasedAt());
            case PAST -> second.lastAppointmentStart().compareTo(first.lastAppointmentStart());
        };
    }

    /** The newest past appointment's start, of a case that has appointments, all of them past. */
    private Instant lastAppointmentStart() {
        return Appointment.past(appointments, asOf).getFirst().start();
    }
}
