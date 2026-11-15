package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.consultation.value.NoteVisibility;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A note as its case's assigned expert sees it; {@code editedAt} is {@code null} until an edit. */
public record ExpertNoteResponse(
    UUID id,
    String title,
    String text,
    NoteVisibility visibility,
    Instant writtenAt,
    @Nullable Instant editedAt) {

  public static ExpertNoteResponse from(Note note) {
    return new ExpertNoteResponse(
        note.id().value(),
        note.title().value(),
        note.text().value(),
        note.visibility(),
        note.writtenAt(),
        note.editedAt().orElse(null));
  }
}
