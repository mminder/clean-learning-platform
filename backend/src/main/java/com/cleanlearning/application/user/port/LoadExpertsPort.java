package com.cleanlearning.application.user.port;

import com.cleanlearning.domain.user.Expert;
import java.util.List;

public interface LoadExpertsPort {

  List<Expert> loadExperts();
}
