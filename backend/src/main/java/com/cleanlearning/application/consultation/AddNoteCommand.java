package com.cleanlearning.application.consultation;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteText;
import com.cleanlearning.domain.consultation.value.NoteTitle;
import com.cleanlearning.domain.consultation.value.NoteVisibility;
import java.util.Objects;

public record AddNoteCommand(
    ConsultationCaseId consultationCaseId,
    NoteTitle title,
    NoteText text,
    NoteVisibility visibility) {

  public AddNoteCommand {
    Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(visibility, "visibility");
  }
}
