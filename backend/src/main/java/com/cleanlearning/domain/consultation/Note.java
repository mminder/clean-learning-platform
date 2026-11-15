package com.cleanlearning.domain.consultation;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.consultation.value.NoteText;
import com.cleanlearning.domain.consultation.value.NoteTitle;
import com.cleanlearning.domain.consultation.value.NoteVisibility;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * A note of a case, which its assigned expert writes, edits and deletes (UC-15). Both times come
 * from the application's clock (spec 06, decision 5). Every save is an edit, also one that changes
 * only the visibility, or nothing.
 */
public record Note(
    NoteId id,
    ConsultationCaseId consultationCaseId,
    NoteTitle title,
    NoteText text,
    NoteVisibility visibility,
    Instant writtenAt,
    Optional<Instant> editedAt) {

  public Note {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(consultationCaseId, "consultationCaseId");
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(text, "text");
    Objects.requireNonNull(visibility, "visibility");
    Objects.requireNonNull(writtenAt, "writtenAt");
    Objects.requireNonNull(editedAt, "editedAt");
  }

  public static Note write(
      ConsultationCaseId consultationCaseId,
      NoteTitle title,
      NoteText text,
      NoteVisibility visibility,
      Instant now) {
    return new Note(
        new NoteId(UUID.randomUUID()),
        consultationCaseId,
        title,
        text,
        visibility,
        now,
        Optional.empty());
  }

  public Note edit(NoteTitle title, NoteText text, NoteVisibility visibility, Instant now) {
    return new Note(id, consultationCaseId, title, text, visibility, writtenAt, Optional.of(now));
  }

  public boolean isPublic() {
    return visibility == NoteVisibility.PUBLIC;
  }
}
