package com.cleanlearning.adapters.database.consultation.entity;

import com.cleanlearning.domain.consultation.value.NoteVisibility;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "note")
public class NoteEntity {

  @Id private UUID id;

  private UUID consultationCaseId;

  private String title;

  private String text;

  @Enumerated(EnumType.STRING)
  private NoteVisibility visibility;

  private Instant writtenAt;

  // Null until the first edit.
  private @Nullable Instant editedAt;

  protected NoteEntity() {}

  public NoteEntity(
      UUID id,
      UUID consultationCaseId,
      String title,
      String text,
      NoteVisibility visibility,
      Instant writtenAt,
      @Nullable Instant editedAt) {
    this.id = id;
    this.consultationCaseId = consultationCaseId;
    this.title = title;
    this.text = text;
    this.visibility = visibility;
    this.writtenAt = writtenAt;
    this.editedAt = editedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getConsultationCaseId() {
    return consultationCaseId;
  }

  public String getTitle() {
    return title;
  }

  public String getText() {
    return text;
  }

  public NoteVisibility getVisibility() {
    return visibility;
  }

  public Instant getWrittenAt() {
    return writtenAt;
  }

  public @Nullable Instant getEditedAt() {
    return editedAt;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setVisibility(NoteVisibility visibility) {
    this.visibility = visibility;
  }

  public void setEditedAt(@Nullable Instant editedAt) {
    this.editedAt = editedAt;
  }
}
