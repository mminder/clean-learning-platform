package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.DeleteNotePort;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The case's assigned expert deletes a note of it for good (UC-15, alternative flow "Delete"). A
 * note that's gone, also one deleted in another tab, is a 404.
 */
@Service
public class DeleteNoteUseCase {

  private final DeleteNotePort deleteNote;

  DeleteNoteUseCase(DeleteNotePort deleteNote) {
    this.deleteNote = deleteNote;
  }

  @Transactional
  public void invoke(CurrentUser user, ConsultationCaseId consultationCaseId, NoteId noteId) {
    if (!deleteNote.delete(noteId, consultationCaseId, user.id())) {
      throw new NotFoundException("Note " + noteId.value() + " not found.");
    }
  }
}
