package com.cleanlearning.api.consultation;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.consultation.request.NoteRequest;
import com.cleanlearning.api.consultation.response.ExpertConsultationCaseResponse;
import com.cleanlearning.api.consultation.response.ExpertConsultationCaseSummaryResponse;
import com.cleanlearning.api.consultation.response.ExpertNoteResponse;
import com.cleanlearning.application.consultation.AddNoteCommand;
import com.cleanlearning.application.consultation.AddNoteUseCase;
import com.cleanlearning.application.consultation.DeleteNoteUseCase;
import com.cleanlearning.application.consultation.EditNoteCommand;
import com.cleanlearning.application.consultation.EditNoteUseCase;
import com.cleanlearning.application.consultation.GetExpertConsultationCaseUseCase;
import com.cleanlearning.application.consultation.ListExpertConsultationCasesUseCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.consultation.value.NoteText;
import com.cleanlearning.domain.consultation.value.NoteTitle;
import com.cleanlearning.domain.user.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The cases of the current user as their assigned expert (UC-11), with their notes (UC-15). */
@RestController
@RequestMapping(ExpertConsultationCaseController.PATH)
@PreAuthorize("hasRole('EXPERT')")
class ExpertConsultationCaseController {

  static final String PATH = "/api/expert/consultation-cases";

  private final ListExpertConsultationCasesUseCase listExpertConsultationCases;

  private final GetExpertConsultationCaseUseCase getExpertConsultationCase;

  private final AddNoteUseCase addNote;

  private final EditNoteUseCase editNote;

  private final DeleteNoteUseCase deleteNote;

  ExpertConsultationCaseController(
      ListExpertConsultationCasesUseCase listExpertConsultationCases,
      GetExpertConsultationCaseUseCase getExpertConsultationCase,
      AddNoteUseCase addNote,
      EditNoteUseCase editNote,
      DeleteNoteUseCase deleteNote) {
    this.listExpertConsultationCases = listExpertConsultationCases;
    this.getExpertConsultationCase = getExpertConsultationCase;
    this.addNote = addNote;
    this.editNote = editNote;
    this.deleteNote = deleteNote;
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

  /**
   * A new note has no route of its own, it's part of its case, so the answer has no {@code
   * Location}. Another expert's case answers 404, like an unknown one.
   */
  @PostMapping("/{id}/notes")
  @ResponseStatus(HttpStatus.CREATED)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  @ProblemResponses.Status415
  ExpertNoteResponse addNote(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID id,
      @Valid @RequestBody NoteRequest request) {
    return ExpertNoteResponse.from(
        addNote.invoke(
            currentUser,
            new AddNoteCommand(
                new ConsultationCaseId(id),
                new NoteTitle(request.title()),
                new NoteText(request.text()),
                request.visibility())));
  }

  /** A note of another case, of another expert's case, or one that's gone answers 404. */
  @PutMapping("/{id}/notes/{noteId}")
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  @ProblemResponses.Status415
  ExpertNoteResponse editNote(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID id,
      @PathVariable UUID noteId,
      @Valid @RequestBody NoteRequest request) {
    return ExpertNoteResponse.from(
        editNote.invoke(
            currentUser,
            new EditNoteCommand(
                new ConsultationCaseId(id),
                new NoteId(noteId),
                new NoteTitle(request.title()),
                new NoteText(request.text()),
                request.visibility())));
  }

  /** A note of another case, of another expert's case, or one that's gone answers 404. */
  @DeleteMapping("/{id}/notes/{noteId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  void deleteNote(
      @AuthenticationPrincipal CurrentUser currentUser,
      @PathVariable UUID id,
      @PathVariable UUID noteId) {
    deleteNote.invoke(currentUser, new ConsultationCaseId(id), new NoteId(noteId));
  }
}
