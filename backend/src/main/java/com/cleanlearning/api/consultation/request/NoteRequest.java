package com.cleanlearning.api.consultation.request;

import com.cleanlearning.application.consultation.AddNoteUseCase;
import com.cleanlearning.application.consultation.EditNoteUseCase;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.consultation.value.NoteText;
import com.cleanlearning.domain.consultation.value.NoteTitle;
import com.cleanlearning.domain.consultation.value.NoteVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The annotations repeat UC-15's rules so a 400 names every invalid field; the value objects stay
 * the authority.
 */
public record NoteRequest(
    @NotBlank String title, @NotBlank String text, @NotNull NoteVisibility visibility) {

  public AddNoteUseCase.Command toAddCommand(ConsultationCaseId consultationCaseId) {
    return new AddNoteUseCase.Command(
        consultationCaseId, new NoteTitle(title), new NoteText(text), visibility);
  }

  public EditNoteUseCase.Command toEditCommand(
      ConsultationCaseId consultationCaseId, NoteId noteId) {
    return new EditNoteUseCase.Command(
        consultationCaseId, noteId, new NoteTitle(title), new NoteText(text), visibility);
  }
}
