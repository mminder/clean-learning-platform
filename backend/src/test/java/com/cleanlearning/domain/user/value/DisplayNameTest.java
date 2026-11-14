package com.cleanlearning.domain.user.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;

class DisplayNameTest {

  @Test
  void holdsTheName() {
    assertThat(new DisplayName("Expert 1").value()).isEqualTo("Expert 1");
  }

  @Test
  void rejectsEmptyAndBlankNames() {
    assertThatExceptionOfType(InvalidValueException.class).isThrownBy(() -> new DisplayName(""));
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new DisplayName(" \t\n"));
  }

  @Test
  void rejectsNamesOfNoBreakSpaces() {
    // String.isBlank() accepts these, the database's check constraint doesn't.
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new DisplayName("   \u0085"));
  }

  @Test
  void isRequired() {
    assertThatNullPointerException().isThrownBy(() -> new DisplayName(null));
  }

  @Test
  void isComparedByValue() {
    assertThat(new DisplayName("Expert 1")).isEqualTo(new DisplayName("Expert 1"));
    assertThat(new DisplayName("Expert 1")).isNotEqualTo(new DisplayName("Expert 2"));
  }
}
