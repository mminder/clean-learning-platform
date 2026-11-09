package com.cleanlearning.domain.consultation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.offering.value.OfferingName;
import com.cleanlearning.domain.offering.value.SessionCount;

/**
 * A case as its customer sees it on "My purchases" (UC-05 step 2): the offering's name and image, read live (D-08),
 * the purchase time and the credits. The session count is the purchase terms' one, which the credits come from.
 */
public record CustomerConsultationCaseSummary(ConsultationCaseId id, OfferingName offeringName,
        Optional<ImageId> offeringImageId, SessionCount sessionCount, Instant purchasedAt) {

    public CustomerConsultationCaseSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(offeringName, "offeringName");
        Objects.requireNonNull(offeringImageId, "offeringImageId");
        Objects.requireNonNull(sessionCount, "sessionCount");
        Objects.requireNonNull(purchasedAt, "purchasedAt");
    }

    /** The purchase terms' session count, until credit adjustments exist (UC-18). */
    public int totalCredits() {
        return sessionCount.value();
    }

    /**
     * Computed, never stored (D-11). TODO task 04 of increment 04: the summary gets its appointments and counts with
     * {@link Appointment#openCredits}; until then every credit counts as open here, while the case detail subtracts
     * its booked appointments.
     */
    public int openCredits() {
        return totalCredits();
    }
}
