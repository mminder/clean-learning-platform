package com.cleanlearning.domain.user.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TeamsLinkTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "https://teams.microsoft.com/l/meetup-join/19%3ameeting_abc%40thread.v2/0?context=%7b%7d",
        "https://example.com/meet/expert-2",
        "https://x"
      })
  void holdsAnyHttpsLink(String link) {
    assertThat(new TeamsLink(link).value()).isEqualTo(link);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "https://",
        "http://example.com",
        "example.com",
        "HTTPS://example.com",
        " https://example.com",
        "https://example.com/meet expert-2",
        "https://example.com/\tmeet",
        "https://example.com/ meet",
        "https://exämple.com",
        "https://example.com/\n",
        "javascript:alert(1)"
      })
  void rejectsAnythingButAnHttpsLinkWithoutSpaces(String link) {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new TeamsLink(link))
        .withMessage("A Teams link is an https:// link.");
  }

  @Test
  void isRequired() {
    assertThatNullPointerException().isThrownBy(() -> new TeamsLink(null));
  }

  @Test
  void isComparedByValue() {
    assertThat(new TeamsLink("https://example.com/a"))
        .isEqualTo(new TeamsLink("https://example.com/a"));
    assertThat(new TeamsLink("https://example.com/a"))
        .isNotEqualTo(new TeamsLink("https://example.com/b"));
  }
}
