package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.CaseCancellation;

public interface SaveCaseCancellationPort {

    /**
     * Sets the case to {@code CANCELLED}, with when and whether it's refund-eligible, and each appointment cancelled
     * with it to {@code CANCELLED}, with whether its credit was refunded. The case was locked in the same transaction
     * ({@link LockCaseForCancellationPort}), which loaded it and its appointments.
     */
    void save(CaseCancellation cancellation);
}
