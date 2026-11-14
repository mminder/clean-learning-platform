package com.cleanlearning.api.consultation;

import com.cleanlearning.api.consultation.response.ExpertConsultationCaseSummaryResponse;
import com.cleanlearning.application.consultation.ListExpertConsultationCasesUseCase;
import com.cleanlearning.domain.user.CurrentUser;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The cases of the current user as their assigned expert (UC-11). */
@RestController
@RequestMapping(ExpertConsultationCaseController.PATH)
@PreAuthorize("hasRole('EXPERT')")
class ExpertConsultationCaseController {

  static final String PATH = "/api/expert/consultation-cases";

  private final ListExpertConsultationCasesUseCase listExpertConsultationCases;

  ExpertConsultationCaseController(ListExpertConsultationCasesUseCase listExpertConsultationCases) {
    this.listExpertConsultationCases = listExpertConsultationCases;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  List<ExpertConsultationCaseSummaryResponse> listExpertConsultationCases(
      @AuthenticationPrincipal CurrentUser currentUser) {
    return listExpertConsultationCases.invoke(currentUser).stream()
        .map(ExpertConsultationCaseSummaryResponse::from)
        .toList();
  }
}
