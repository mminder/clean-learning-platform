package com.cleanlearning.api.consultation;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.consultation.response.ExpertConsultationCaseResponse;
import com.cleanlearning.api.consultation.response.ExpertConsultationCaseSummaryResponse;
import com.cleanlearning.application.consultation.GetExpertConsultationCaseUseCase;
import com.cleanlearning.application.consultation.ListExpertConsultationCasesUseCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.user.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  private final GetExpertConsultationCaseUseCase getExpertConsultationCase;

  ExpertConsultationCaseController(
      ListExpertConsultationCasesUseCase listExpertConsultationCases,
      GetExpertConsultationCaseUseCase getExpertConsultationCase) {
    this.listExpertConsultationCases = listExpertConsultationCases;
    this.getExpertConsultationCase = getExpertConsultationCase;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  List<ExpertConsultationCaseSummaryResponse> listExpertConsultationCases(
      @AuthenticationPrincipal CurrentUser currentUser) {
    return listExpertConsultationCases.invoke(currentUser).stream()
        .map(ExpertConsultationCaseSummaryResponse::from)
        .toList();
  }

  /** Another expert's case answers 404, like an unknown one. */
  @GetMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  ExpertConsultationCaseResponse getExpertConsultationCase(
      @AuthenticationPrincipal CurrentUser currentUser, @PathVariable UUID id) {
    return ExpertConsultationCaseResponse.from(
        getExpertConsultationCase.invoke(currentUser, new ConsultationCaseId(id)));
  }
}
