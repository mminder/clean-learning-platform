package com.cleanlearning.application.user;

import com.cleanlearning.application.user.port.LoadExpertsPort;
import com.cleanlearning.domain.user.Expert;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
