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
 * @param forfeitedCredits the case's appointments cancelled late
 * @param cancelledAt when the case was cancelled (UC-08), empty for an active case
 * @param asOf the time the case is shown at, from the {@code Clock}
 */
public record CustomerConsultationCaseSummary(ConsultationCaseId id, OfferingName offeringName,
        Optional<ImageId> offeringImageId, SessionCount sessionCount, Instant purchasedAt,
        Optional<DisplayName> expertDisplayName, List<Appointment> appointments, int forfeitedCredits,
        Optional<Instant> cancelledAt, Instant asOf) {

    /**
     * The order of "My purchases": by group, then Upcoming by the next appointment, soonest first, Needs booking by
     * purchase time, oldest first, and Past by the cancellation time of a cancelled case, by the last appointment of a
     * completed one, or by the purchase time without one, newest first (spec 05, decision 8); then by id (D-17), as
     * text: that's Postgres's order of a uuid, which {@code UUID.compareTo} (signed) isn't.
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
        if (forfeitedCredits < 0) {
            throw new IllegalArgumentException(
                    "A case has no negative number of forfeited credits: " + forfeitedCredits);
        }
        Objects.requireNonNull(cancelledAt, "cancelledAt");
        Objects.requireNonNull(asOf, "asOf");
    }

    /** The purchase terms' session count, until credit adjustments exist (UC-18). */
    public int totalCredits() {
        return sessionCount.value();
    }

    /**
     * Every booked appointment uses a credit, consumed or not, and so does every forfeited one, counted as the booking
     * counts them.
     */
    public int openCredits() {
        return Appointment.openCredits(sessionCount, appointments.size(), forfeitedCredits);
    }

    /**
     * The soonest appointment that hasn't ended, {@code CONSUMED} while it runs (spec 04, decision 21); empty without
     * one.
     */
    public Optional<CustomerAppointment> nextAppointment() {
        return Appointment.upcoming(appointments, asOf).stream().findFirst();
    }

    /**
     * Computed, never stored (D-11): {@code PAST} for a cancelled case, whatever it had (spec 05, decision 6);
     * otherwise {@code UPCOMING} with an appointment that hasn't ended, otherwise {@code PAST} once the case is
     * completed, as {@link Appointment#isCompleted} decides for the case detail too, otherwise {@code NEEDS_BOOKING}:
     * an open credit is left.
     */
    public PurchaseGroup group() {
        if (cancelledAt.isPresent()) {
            return PurchaseGroup.PAST;
        }
        if (nextAppointment().isPresent()) {
            return PurchaseGroup.UPCOMING;
        }
        return Appointment.isCompleted(sessionCount, appointments, forfeitedCredits, asOf) ? PurchaseGroup.PAST
                : PurchaseGroup.NEEDS_BOOKING;
    }

    /** Compares two cases of the same group. */
    private static int compareWithinGroup(CustomerConsultationCaseSummary first,
            CustomerConsultationCaseSummary second) {
        return switch (first.group()) {
            case UPCOMING -> first.nextAppointment().orElseThrow().start()
                    .compareTo(second.nextAppointment().orElseThrow().start());
            case NEEDS_BOOKING -> first.purchasedAt().compareTo(second.purchasedAt());
            case PAST -> second.pastOrderKey().compareTo(first.pastOrderKey());
        };
    }

    /**
     * A past case's key in Past's order (spec 05, decision 8): the cancellation time of a cancelled case; otherwise its
     * newest appointment's start, all of them past; or, without an appointment, because late cancellations forfeited
     * every credit, its purchase time, the last date its customer sees.
     */
    private Instant pastOrderKey() {
        return cancelledAt.orElseGet(this::completedOrderKey);
    }

    private Instant completedOrderKey() {
        return Appointment.past(appointments, asOf).stream().findFirst().map(CustomerAppointment::start)
                .orElse(purchasedAt);
    }
}
