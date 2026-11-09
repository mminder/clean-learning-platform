package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.PurchaseTerms;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingName;

/**
 * A case as its customer sees it at {@code asOf} (UC-05's case detail): the offering's name and image, read live
 * (D-08), the purchase terms, the purchase time, the assigned expert, the appointments and the credits. What depends on
 * the time is derived at {@code asOf}, never stored (D-11); {@link Appointment} holds the appointments' rules. Every
 * case is active until UC-08 exists.
 *
 * @param expert the assigned expert, empty until the first booking
 * @param appointments the case's {@code BOOKED} appointments, in any order
 * @param asOf the time the case is shown at, from the {@code Clock}
 */
public record CustomerConsultationCase(ConsultationCaseId id, OfferingName offeringName,
        Optional<ImageId> offeringImageId, PurchaseTerms purchaseTerms, Instant purchasedAt,
        Optional<AssignedExpert> expert, List<Appointment> appointments, Instant asOf) {

    public CustomerConsultationCase {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(offeringName, "offeringName");
        Objects.requireNonNull(offeringImageId, "offeringImageId");
        Objects.requireNonNull(purchaseTerms, "purchaseTerms");
        Objects.requireNonNull(purchasedAt, "purchasedAt");
        Objects.requireNonNull(expert, "expert");
        appointments = List.copyOf(appointments);
        Objects.requireNonNull(asOf, "asOf");
    }

    /** The purchase terms' session count, until credit adjustments exist (UC-18). */
    public int totalCredits() {
        return purchaseTerms.sessionCount().value();
    }

    /** Every booked appointment uses a credit, consumed or not, counted as the booking counts them. */
    public int openCredits() {
        return Appointment.openCredits(purchaseTerms.sessionCount(), appointments.size());
    }

    /** Whether the customer can book an appointment: an open credit is left. UC-08 adds "and the case is active". */
    public boolean canBook() {
        return openCredits() > 0;
    }

    /** The appointments that haven't ended, soonest first; a running one is {@code CONSUMED} (spec 04, decision 21). */
    public List<CustomerAppointment> upcomingAppointments() {
        return Appointment.upcoming(appointments, asOf);
    }

    /** The appointments that have ended, newest first, all {@code CONSUMED}. */
    public List<CustomerAppointment> pastAppointments() {
        return Appointment.past(appointments, asOf);
    }
}
