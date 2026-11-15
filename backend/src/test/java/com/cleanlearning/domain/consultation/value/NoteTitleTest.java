package com.cleanlearning.domain.consultation.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;

class NoteTitleTest {

  @Test
  void holdsTheTitle() {
    assertThat(new NoteTitle("First session").value()).isEqualTo("First session");
  }

  @Test
  void rejectsEmptyAndBlankTitles() {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new NoteTitle(""))
        .withMessage("A note's title is not blank.");
    assertThatExceptionOfType(InvalidValueException.class).isThrownBy(() -> new NoteTitle(" \t\n"));
  }

  @Test
  void rejectsTitlesOfNoBreakSpaces() {
    // String.isBlank() accepts these, the database's check constraint doesn't.
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new NoteTitle("   \u0085"));
  }

  @Test
  void isRequired() {
    assertThatNullPointerException().isThrownBy(() -> new NoteTitle(null));
  }
}
