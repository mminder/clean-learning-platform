package com.cleanlearning.domain.offering;

import java.util.Objects;

import com.cleanlearning.domain.offering.value.ConsultationPackageId;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.shared.value.Money;

/**
 * A package of a consultation offering: a number of sessions for a price, for example 3 sessions for CHF 450.
 */
public record ConsultationPackage(ConsultationPackageId id, SessionCount sessionCount, Money price) {

    public ConsultationPackage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sessionCount, "sessionCount");
        Objects.requireNonNull(price, "price");
    }
}
