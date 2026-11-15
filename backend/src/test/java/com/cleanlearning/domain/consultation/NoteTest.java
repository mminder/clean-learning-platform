package com.cleanlearning.domain.consultation;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.domain.consultation.value.ConsultationCaseId;
import com.cleanlearning.domain.consultation.value.NoteText;
import com.cleanlearning.domain.consultation.value.NoteTitle;
import com.cleanlearning.domain.consultation.value.NoteVisibility;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NoteTest {

  private static final ConsultationCaseId CASE_ID = new ConsultationCaseId(UUID.randomUUID());

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant WRITTEN = Instant.parse("2026-11-09T14:00:00Z");

  /** Tue 10 Nov 2026, 09:00 in Zurich. */
  private static final Instant EDITED = Instant.parse("2026-11-10T08:00:00Z");

  private static final NoteTitle TITLE = new NoteTitle("First session");

  private static final NoteText TEXT = new NoteText("Line one\nLine two");

  private static Note written() {
    return Note.write(CASE_ID, TITLE, TEXT, NoteVisibility.PRIVATE, WRITTEN);
  }

  @Test
  void aNewNoteIsWrittenNowAndNeverEdited() {
    Note note = written();

    assertThat(note.id()).isNotNull();
    assertThat(note.consultationCaseId()).isEqualTo(CASE_ID);
    assertThat(note.title()).isEqualTo(TITLE);
    assertThat(note.text()).isEqualTo(TEXT);
    assertThat(note.visibility()).isEqualTo(NoteVisibility.PRIVATE);
    assertThat(note.writtenAt()).isEqualTo(WRITTEN);
    assertThat(note.editedAt()).isEmpty();
    assertThat(note.isPublic()).isFalse();
  }

  @Test
  void eachNewNoteHasItsOwnId() {
    assertThat(written().id()).isNotEqualTo(written().id());
  }

  @Test
  void anEditChangesTheTitleTextAndVisibilityAndKeepsTheRest() {
    Note note = written();

    Note edited =
        note.edit(
            new NoteTitle("Homework"),
            new NoteText("Read chapter 3"),
            NoteVisibility.PUBLIC,
            EDITED);

    assertThat(edited)
        .isEqualTo(
            new Note(
                note.id(),
                CASE_ID,
                new NoteTitle("Homework"),
                new NoteText("Read chapter 3"),
                NoteVisibility.PUBLIC,
                WRITTEN,
                Optional.of(EDITED)));
    assertThat(edited.isPublic()).isTrue();
  }

  @Test
  void anEditOfTheTitleOnly() {
    Note edited = written().edit(new NoteTitle("Homework"), TEXT, NoteVisibility.PRIVATE, EDITED);

    assertThat(edited.title()).isEqualTo(new NoteTitle("Homework"));
    assertThat(edited.text()).isEqualTo(TEXT);
    assertThat(edited.editedAt()).contains(EDITED);
  }

  @Test
  void anEditOfTheTextOnly() {
    Note edited =
        written().edit(TITLE, new NoteText("Line one\nLine three"), NoteVisibility.PRIVATE, EDITED);

    assertThat(edited.title()).isEqualTo(TITLE);
    assertThat(edited.text()).isEqualTo(new NoteText("Line one\nLine three"));
    assertThat(edited.editedAt()).contains(EDITED);
  }

  /** Spec 06, decision 5: a change of visibility alone is an edit. */
  @Test
  void anEditOfTheVisibilityOnly() {
    Note edited = written().edit(TITLE, TEXT, NoteVisibility.PUBLIC, EDITED);

    assertThat(edited.visibility()).isEqualTo(NoteVisibility.PUBLIC);
    assertThat(edited.writtenAt()).isEqualTo(WRITTEN);
    assertThat(edited.editedAt()).contains(EDITED);
  }

  /** Spec 06, decision 5: every save is an edit, so the domain compares no texts. */
  @Test
  void aSaveThatChangesNothingIsAnEditToo() {
    Note note = written();

    Note edited = note.edit(TITLE, TEXT, NoteVisibility.PRIVATE, EDITED);

    assertThat(edited.editedAt()).contains(EDITED);
    assertThat(edited.id()).isEqualTo(note.id());
  }

  @Test
  void theLastEditCounts() {
    Note edited =
        written()
            .edit(TITLE, TEXT, NoteVisibility.PUBLIC, EDITED)
            .edit(TITLE, TEXT, NoteVisibility.PRIVATE, EDITED.plusSeconds(60));

    assertThat(edited.editedAt()).contains(EDITED.plusSeconds(60));
    assertThat(edited.writtenAt()).isEqualTo(WRITTEN);
  }
}
