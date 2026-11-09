package com.cleanlearning.application.consultation.port;

import java.util.List;

import com.cleanlearning.domain.consultation.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.user.value.UserId;

public interface LoadCustomerConsultationCasesPort {

    /**
     * The customer's own cases, whatever their offerings' status, by purchase time and then id. The query is scoped to
     * the customer, so another customer's case never shows (security.md).
     */
    List<CustomerConsultationCaseSummary> loadAll(UserId customerId);
}
