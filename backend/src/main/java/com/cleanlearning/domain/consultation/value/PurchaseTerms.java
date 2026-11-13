package com.cleanlearning.domain.consultation.value;

import com.cleanlearning.domain.offering.value.CancellationDeadline;
import com.cleanlearning.domain.offering.value.SessionCount;
import com.cleanlearning.domain.offering.value.SessionDuration;
import com.cleanlearning.domain.shared.value.Money;
import java.util.Objects;

/**
 * What a case's customer bought (D-08): the price paid and the session count of the package, and
 * the offering's session duration and cancellation deadline, copied at the purchase. Later edits of
 * the offering never change them.
 */
public record PurchaseTerms(
    Money pricePaid,
    SessionCount sessionCount,
    SessionDuration sessionDuration,
    CancellationDeadline cancellationDeadline) {

  public PurchaseTerms {
    Objects.requireNonNull(pricePaid, "pricePaid");
    Objects.requireNonNull(sessionCount, "sessionCount");
    Objects.requireNonNull(sessionDuration, "sessionDuration");
    Objects.requireNonNull(cancellationDeadline, "cancellationDeadline");
  }
}
