package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadNotePort;
import com.cleanlearning.application.consultation.port.SaveNotePort;
import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The case's assigned expert edits a note of it (UC-15, alternative flow "Edit"). No lock: the last
 * save wins, and a note that's gone is a 404 (spec 06, decision 7).
 */
@Service
public class EditNoteUseCase {

  private final LoadNotePort loadNote;

  private final SaveNotePort saveNote;

  private final Clock clock;

  EditNoteUseCase(LoadNotePort loadNote, SaveNotePort saveNote, Clock clock) {
    this.loadNote = loadNote;
    this.saveNote = saveNote;
    this.clock = clock;
  }

  @Transactional
  public Note invoke(CurrentUser user, EditNoteCommand command) {
    Note note =
        loadNote
            .load(command.noteId(), command.consultationCaseId(), user.id())
            .orElseThrow(
                () -> new NotFoundException("Note " + command.noteId().value() + " not found."));
    Note edited = note.edit(command.title(), command.text(), command.visibility(), clock.instant());
    saveNote.update(edited);
    return edited;
  }
}
