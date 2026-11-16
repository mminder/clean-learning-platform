package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.LoadNotePort;
import com.cleanlearning.application.consultation.port.SaveNotePort;
import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.consultation.value.NoteText;
import com.cleanlearning.domain.consultation.value.NoteTitle;
import com.cleanlearning.domain.consultation.value.NoteVisibility;
import com.cleanlearning.domain.shared.NotFoundException;
import com.cleanlearning.domain.user.CurrentUser;
import java.time.Clock;
import java.util.Objects;
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
  public Note invoke(CurrentUser user, Command command) {
    Note note =
        loadNote
            .load(command.noteId(), command.consultationCaseId(), user.id())
            .orElseThrow(
                () -> new NotFoundException("Note " + command.noteId().value() + " not found."));
    Note edited = note.edit(command.title(), command.text(), command.visibility(), clock.instant());
    saveNote.update(edited);
    return edited;
  }

  public record Command(
      ConsultationCaseId consultationCaseId,
      NoteId noteId,
      NoteTitle title,
      NoteText text,
      NoteVisibility visibility) {

    public Command {
      Objects.requireNonNull(consultationCaseId, "consultationCaseId");
      Objects.requireNonNull(noteId, "noteId");
      Objects.requireNonNull(title, "title");
      Objects.requireNonNull(text, "text");
      Objects.requireNonNull(visibility, "visibility");
    }
  }
}
