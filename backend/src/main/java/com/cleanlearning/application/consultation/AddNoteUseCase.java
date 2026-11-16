package com.cleanlearning.application.consultation;

import com.cleanlearning.application.consultation.port.CheckAssignedExpertPort;
import com.cleanlearning.application.consultation.port.SaveNotePort;
import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
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
 * The case's assigned expert writes a note on it (UC-15 steps 1 to 3), in any status of the case;
 * any other case is a 404.
 */
@Service
public class AddNoteUseCase {

  private final CheckAssignedExpertPort checkAssignedExpert;

  private final SaveNotePort saveNote;

  private final Clock clock;

  AddNoteUseCase(CheckAssignedExpertPort checkAssignedExpert, SaveNotePort saveNote, Clock clock) {
    this.checkAssignedExpert = checkAssignedExpert;
    this.saveNote = saveNote;
    this.clock = clock;
  }

  @Transactional
  public Note invoke(CurrentUser user, Command command) {
    if (!checkAssignedExpert.isAssignedExpert(command.consultationCaseId(), user.id())) {
      throw new NotFoundException("Case " + command.consultationCaseId().value() + " not found.");
    }
    Note note =
        Note.write(
            command.consultationCaseId(),
            command.title(),
            command.text(),
            command.visibility(),
            clock.instant());
    saveNote.add(note);
    return note;
  }

  public record Command(
      ConsultationCaseId consultationCaseId,
      NoteTitle title,
      NoteText text,
      NoteVisibility visibility) {

    public Command {
      Objects.requireNonNull(consultationCaseId, "consultationCaseId");
      Objects.requireNonNull(title, "title");
      Objects.requireNonNull(text, "text");
      Objects.requireNonNull(visibility, "visibility");
    }
  }
}
