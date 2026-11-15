package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.user.value.UserId;

public interface DeleteNotePort {

  /**
   * Deletes the note only if it's a note of this case and {@code expertId} is the case's assigned
   * expert, and answers whether it deleted one.
   */
  boolean delete(NoteId id, ConsultationCaseId consultationCaseId, UserId expertId);
}
