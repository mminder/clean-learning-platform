package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCaseForBookingPort;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.ExpertProfile;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** From the case, not from the shop: an archived offering's case still books. */
@Service
public class ListEligibleExpertsForCaseUseCase {

  private final LoadCaseForBookingPort loadCase;

  ListEligibleExpertsForCaseUseCase(LoadCaseForBookingPort loadCase) {
    this.loadCase = loadCase;
  }

  @Transactional(readOnly = true)
  public List<ExpertProfile> invoke(CurrentUser user, ConsultationCaseId id) {
    CaseForBooking consultationCase =
        loadCase
            .load(id, user.id())
            .orElseThrow(() -> new NotFoundException("Case " + id.value() + " not found."));
    return consultationCase.eligibleExperts();
  }
}
