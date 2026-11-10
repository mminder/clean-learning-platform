package com.cleanlearning.application.consultation;

import java.time.Clock;
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

    private final Clock clock;

    ListCustomerConsultationCasesUseCase(LoadCustomerConsultationCasesPort loadCases, Clock clock) {
        this.loadCases = loadCases;
        this.clock = clock;
    }

    /**
     * The current user's cases at the time of the {@code Clock}, in the order of "My purchases": Upcoming, Needs
     * booking, then Past; empty without purchases.
     */
    @Transactional(readOnly = true)
    public List<CustomerConsultationCaseSummary> invoke(CurrentUser user) {
        return loadCases.loadAll(user.id(), clock.instant()).stream()
                .sorted(CustomerConsultationCaseSummary.MY_PURCHASES_ORDER).toList();
    }
}
