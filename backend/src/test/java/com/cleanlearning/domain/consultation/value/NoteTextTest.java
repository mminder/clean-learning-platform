package com.cleanlearning.domain.consultation.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;

class NoteTextTest {

  @Test
  void holdsTheTextWithItsLineBreaks() {
    assertThat(new NoteText("Line one\nLine two").value()).isEqualTo("Line one\nLine two");
  }

  @Test
  void rejectsEmptyAndBlankTexts() {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new NoteText(""))
        .withMessage("A note's text is not blank.");
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new NoteText(" \t\n\r\n"));
  }

  @Test
  void rejectsTextsOfNoBreakSpaces() {
    // String.isBlank() accepts these, the database's check constraint doesn't.
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new NoteText(" \n  \u0085"));
  }

  @Test
  void isRequired() {
    assertThatNullPointerException().isThrownBy(() -> new NoteText(null));
  }
}
