package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.NoteEntity;
import com.cleanlearning.application.consultation.port.CheckAssignedExpertPort;
import com.cleanlearning.application.consultation.port.DeleteNotePort;
import com.cleanlearning.application.consultation.port.LoadNotePort;
import com.cleanlearning.application.consultation.port.SaveNotePort;
import com.cleanlearning.domain.consultation.Note;
import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteId;
import com.cleanlearning.domain.user.value.UserId;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The notes of a case for its assigned expert: every query is scoped to the case and the expert in
 * SQL. No lock and no version: the last save wins (spec 06, decision 7).
 */
@Component
class NoteAdapter implements CheckAssignedExpertPort, LoadNotePort, SaveNotePort, DeleteNotePort {

  private final NoteRepository repository;

  private final EntityManager entityManager;

  NoteAdapter(NoteRepository repository, EntityManager entityManager) {
    this.repository = repository;
    this.entityManager = entityManager;
  }

  @Override
  public boolean isAssignedExpert(ConsultationCaseId id, UserId expertId) {
    return repository.isAssignedExpert(id.value(), expertId.value());
  }

  @Override
  public Optional<Note> load(NoteId id, ConsultationCaseId consultationCaseId, UserId expertId) {
    return repository
        .findExpertNote(id.value(), consultationCaseId.value(), expertId.value())
        .map(ConsultationCaseMapper::toNote);
  }

  /** A new row, so it's persisted, not merged (backend.md, persistence). */
  @Override
  public void add(Note note) {
    entityManager.persist(
        new NoteEntity(
            note.id().value(),
            note.consultationCaseId().value(),
            note.title().value(),
            note.text().value(),
            note.visibility(),
            note.writtenAt(),
            note.editedAt().orElse(null)));
  }

  /** Changes the managed entity that {@link #load} read in the same transaction. */
  @Override
  public void update(Note note) {
    NoteEntity entity = entityManager.find(NoteEntity.class, note.id().value());
    if (entity == null) {
      throw new IllegalStateException("Note " + note.id().value() + " doesn't exist.");
    }
    entity.setTitle(note.title().value());
    entity.setText(note.text().value());
    entity.setVisibility(note.visibility());
    entity.setEditedAt(note.editedAt().orElse(null));
  }

  @Override
  public boolean delete(NoteId id, ConsultationCaseId consultationCaseId, UserId expertId) {
    return repository.deleteExpertNote(id.value(), consultationCaseId.value(), expertId.value())
        > 0;
  }
}
