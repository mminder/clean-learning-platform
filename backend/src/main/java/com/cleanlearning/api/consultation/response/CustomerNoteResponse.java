package com.cleanlearning.api.consultation.response;

import com.cleanlearning.domain.consultation.Note;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A public note as the case's customer sees it: no visibility, as every one is public (D-14);
 * {@code editedAt} is {@code null} until an edit.
 */
public record CustomerNoteResponse(
    UUID id, String title, String text, Instant writtenAt, @Nullable Instant editedAt) {

  public static CustomerNoteResponse from(Note note) {
    return new CustomerNoteResponse(
        note.id().value(),
        note.title().value(),
        note.text().value(),
        note.writtenAt(),
        note.editedAt().orElse(null));
  }
}
