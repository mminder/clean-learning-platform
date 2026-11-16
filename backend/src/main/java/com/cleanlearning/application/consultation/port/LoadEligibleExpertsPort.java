package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.offering.value.OfferingId;
import com.cleanlearning.domain.user.ExpertProfile;
import java.util.List;

public interface LoadEligibleExpertsPort {

  /** The offering isn't locked: eligibility is read when the booking runs. */
  List<ExpertProfile> load(OfferingId offeringId);
}
