package com.cleanlearning.adapters.database.consultation;

import com.cleanlearning.adapters.database.consultation.entity.NoteEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface NoteRepository extends Repository<NoteEntity, UUID> {

  /** Newest first; an edit doesn't move a note (UC-15). Ties break by id, descending. */
  @Query(
      """
      select n from NoteEntity n
      where n.consultationCaseId = :consultationCaseId
      order by n.writtenAt desc, n.id desc
      """)
  List<NoteEntity> findNotes(UUID consultationCaseId);

  /**
   * The customer's: only the public notes, filtered in SQL, so a private one never leaves the
   * database (D-14). In the order of {@link #findNotes}.
   */
  @Query(
      """
      select n from NoteEntity n
      where n.consultationCaseId = :consultationCaseId
        and n.visibility = com.cleanlearning.domain.consultation.value.NoteVisibility.PUBLIC
      order by n.writtenAt desc, n.id desc
      """)
  List<NoteEntity> findPublicNotes(UUID consultationCaseId);

  /** Scoped to the case and its assigned expert in SQL. */
  @Query(
      """
      select n from NoteEntity n
      join ConsultationCaseEntity c on c.id = n.consultationCaseId
      where n.id = :id and c.id = :consultationCaseId and c.assignedExpertId = :expertId
      """)
  Optional<NoteEntity> findExpertNote(UUID id, UUID consultationCaseId, UUID expertId);

  /** Scoped to the case and its assigned expert in SQL; answers how many it deleted. */
  @Modifying
  @Query(
      """
      delete from NoteEntity n
      where n.id = :id
        and n.consultationCaseId in (
          select c.id from ConsultationCaseEntity c
          where c.id = :consultationCaseId and c.assignedExpertId = :expertId)
      """)
  int deleteExpertNote(UUID id, UUID consultationCaseId, UUID expertId);

  @Query(
      """
      select count(c) > 0 from ConsultationCaseEntity c
      where c.id = :consultationCaseId and c.assignedExpertId = :expertId
      """)
  boolean isAssignedExpert(UUID consultationCaseId, UUID expertId);
}
