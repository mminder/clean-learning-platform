package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;

public interface LoadNotePort {

  /**
   * The note only if it's a note of this case and {@code expertId} is the case's assigned expert: a
   * note of another case, of another expert's case and an unknown one are all empty.
   */
  Optional<Note> load(NoteId id, ConsultationCaseId consultationCaseId, UserId expertId);
}
