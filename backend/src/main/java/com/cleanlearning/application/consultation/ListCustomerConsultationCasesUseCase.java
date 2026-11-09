package com.cleanlearning.application.consultation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.consultation.port.LoadCustomerConsultationCasesPort;
import com.cleanlearning.domain.consultation.CustomerConsultationCaseSummary;
import com.cleanlearning.domain.user.CurrentUser;

/**
 * UC-05: the customer's landing page, "My purchases".
 */
@Service
public class ListCustomerConsultationCasesUseCase {

    private final LoadCustomerConsultationCasesPort loadCases;

    ListCustomerConsultationCasesUseCase(LoadCustomerConsultationCasesPort loadCases) {
        this.loadCases = loadCases;
    }

    /** The current user's cases, the oldest purchase first; empty without purchases. */
    @Transactional(readOnly = true)
    public List<CustomerConsultationCaseSummary> invoke(CurrentUser user) {
        return loadCases.loadAll(user.id());
    }
}
