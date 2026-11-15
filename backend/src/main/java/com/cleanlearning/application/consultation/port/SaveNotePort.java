package com.cleanlearning.application.consultation.port;

import com.cleanlearning.domain.consultation.Note;

public interface SaveNotePort {

  void add(Note note);

  /** A note that {@link LoadNotePort} loaded in the same transaction. */
  void update(Note note);
}
