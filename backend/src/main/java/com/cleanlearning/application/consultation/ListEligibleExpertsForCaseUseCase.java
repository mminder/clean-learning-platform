package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadCaseForBookingPort;
import com.cleanlearning.domain.consultation.CaseForBooking;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.offering.EligibleExpert;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-06 step 2: on the first booking, the customer picks their expert from the case's offering's
 * current eligible experts. They come from the case, not from the shop: an archived offering's case
 * still books (spec 04, decision 16).
 */
@Service
public class ListEligibleExpertsForCaseUseCase {

  private final LoadCaseForBookingPort loadCase;

  ListEligibleExpertsForCaseUseCase(LoadCaseForBookingPort loadCase) {
    this.loadCase = loadCase;
  }

  /**
   * The eligible experts of the case's offering, by display name; empty if it has none (spec 04,
   * decision 2).
   *
   * @throws NotFoundException if there's no case with this id, or it's another customer's
   */
  @Transactional(readOnly = true)
  public List<EligibleExpert> invoke(CurrentUser user, ConsultationCaseId id) {
    CaseForBooking consultationCase =
        loadCase
            .load(id, user.id())
            .orElseThrow(() -> new NotFoundException("Case " + id.value() + " not found."));
    return consultationCase.eligibleExperts();
  }
}
