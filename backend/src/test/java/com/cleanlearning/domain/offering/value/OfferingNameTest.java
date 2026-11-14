package com.cleanlearning.domain.offering.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;

class OfferingNameTest {

  @Test
  void holdsTheName() {
    assertThat(new OfferingName("Career Coaching").value()).isEqualTo("Career Coaching");
  }

  @Test
  void rejectsEmptyAndBlankNames() {
    assertThatExceptionOfType(InvalidValueException.class).isThrownBy(() -> new OfferingName(""));
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new OfferingName(" \t\n"));
  }

  @Test
  void rejectsNamesOfNoBreakSpaces() {
    // String.isBlank() accepts these, the database's check constraint doesn't.
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new OfferingName("   \u0085"));
  }

  @Test
  void isRequired() {
    assertThatNullPointerException().isThrownBy(() -> new OfferingName(null));
  }

  @Test
  void isComparedByValue() {
    assertThat(new OfferingName("Career Coaching")).isEqualTo(new OfferingName("Career Coaching"));
    assertThat(new OfferingName("Career Coaching"))
        .isNotEqualTo(new OfferingName("Software Architecture"));
  }
}
