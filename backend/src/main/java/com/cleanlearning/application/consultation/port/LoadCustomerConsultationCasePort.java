package com.cleanlearning.application.consultation.port;

import java.time.Instant;
import java.util.Optional;

import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;

public interface LoadCustomerConsultationCasePort {

    /**
     * The customer's case with its assigned expert and its {@code BOOKED} appointments, whatever its offering's status,
     * as shown at {@code asOf}; or empty if there's none with this id or it's another customer's: the query is scoped
     * to the customer, so both look the same (security.md). The case derives what depends on the time from
     * {@code asOf}; the query doesn't.
     */
    Optional<CustomerConsultationCase> load(ConsultationCaseId id, UserId customerId, Instant asOf);
}
