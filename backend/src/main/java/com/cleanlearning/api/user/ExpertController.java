package com.cleanlearning.api.user;

import com.cleanlearning.api.user.response.ExpertResponse;
import com.cleanlearning.application.user.ListExpertsUseCase;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
class ExpertController {

  private final ListExpertsUseCase listExperts;

  ExpertController(ListExpertsUseCase listExperts) {
    this.listExperts = listExperts;
  }

  @GetMapping("/api/admin/experts")
  List<ExpertResponse> listExperts() {
    return listExperts.invoke().stream().map(ExpertResponse::from).toList();
  }
}
