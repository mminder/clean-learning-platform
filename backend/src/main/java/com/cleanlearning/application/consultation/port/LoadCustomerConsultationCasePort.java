package com.cleanlearning.application.consultation.port;

import java.util.Optional;

import com.cleanlearning.domain.consultation.CustomerConsultationCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.value.UserId;

public interface LoadCustomerConsultationCasePort {

    /**
     * The customer's case, whatever its offering's status, or empty if there's none with this id or it's another
     * customer's: the query is scoped to the customer, so both look the same (security.md).
     */
    Optional<CustomerConsultationCase> load(ConsultationCaseId id, UserId customerId);
}
