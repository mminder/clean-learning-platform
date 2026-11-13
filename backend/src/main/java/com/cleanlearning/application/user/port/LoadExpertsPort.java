package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.Expert;
import java.util.List;

public interface LoadExpertsPort {

  /** Every user with {@code EXPERT} and an expert profile, sorted by display name, then id. */
  List<Expert> loadExperts();
}
