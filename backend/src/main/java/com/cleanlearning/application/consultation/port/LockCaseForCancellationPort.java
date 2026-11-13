package com.cleanlearning.application.consultation.port;

import java.util.Optional;

import com.cleanlearning.domain.consultation.CaseForCancellation;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;

public interface LockCaseForCancellationPort {

    /**
     * Locks the customer's case row until the transaction ends, waiting for every other lock of the row, and then
     * loads the case as a cancellation needs it, with all its appointments, booked and cancelled, whatever its
     * offering's status; or returns empty, locking nothing, if there's no case with this id or it's another
     * customer's. The query is scoped to the customer, so only the customer's own case is ever locked (security.md).
     * The case is the only lock of a cancellation (D-27), so a cancellation and a booking of one case, or two
     * cancellations, run one after the other, and the second sees what the first wrote. Not named {@code lock}: the
     * adapter's {@code LockCaseForBookingPort.lock} has the same parameters.
     */
    Optional<CaseForCancellation> lockForCancellation(ConsultationCaseId id, UserId customerId);
}
