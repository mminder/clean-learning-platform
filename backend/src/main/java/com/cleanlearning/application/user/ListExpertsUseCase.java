package com.cleanlearning.application.user;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.user.Expert;

/**
 * UC-16 step 3: the experts an admin can make eligible for a consultation offering, sorted by display name.
 */
@Service
public class ListExpertsUseCase {

    private final LoadExpertsPort loadExperts;

    ListExpertsUseCase(LoadExpertsPort loadExperts) {
        this.loadExperts = loadExperts;
    }

    @Transactional(readOnly = true)
    public List<Expert> invoke() {
        return loadExperts.loadExperts();
    }
}
