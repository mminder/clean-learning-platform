package com.cleanlearning.domain.offering;

import java.util.Objects;
import java.util.Optional;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;

/**
 * A package as the admin entered it in the offering's form (UC-16 step 3): with the id of the package it keeps, or
 * without an id for a new package. {@link ConsultationOffering} matches the entries to its packages by id.
 */
public record ConsultationPackageEntry(Optional<ConsultationPackageId> id, SessionCount sessionCount, Money price) {

    public ConsultationPackageEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sessionCount, "sessionCount");
        Objects.requireNonNull(price, "price");
    }

    /** An entry for the package with this id, with its new values. */
    public static ConsultationPackageEntry kept(ConsultationPackageId id, SessionCount sessionCount, Money price) {
        return new ConsultationPackageEntry(Optional.of(id), sessionCount, price);
    }

    /** An entry for a new package. */
    public static ConsultationPackageEntry added(SessionCount sessionCount, Money price) {
        return new ConsultationPackageEntry(Optional.empty(), sessionCount, price);
    }
}
