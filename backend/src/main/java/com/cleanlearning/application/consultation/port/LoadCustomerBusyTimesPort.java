package com.cleanlearning.application.consultation.port;

import java.time.Instant;
import java.util.List;

import com.cleanlearning.domain.consultation.value.BusyTime;
import com.cleanlearning.domain.user.value.UserId;

public interface LoadCustomerBusyTimesPort {

    /**
     * The customer's busy time that ends after {@code from}, in any order: the {@code BOOKED} appointments of all their
     * cases, whoever the expert is, for a customer's appointments never overlap (spec 04, decision 22). Scoped to the
     * customer: another customer's appointments are never in it.
     */
    List<BusyTime> loadCustomerBusyTimes(UserId customerId, Instant from);
}
