package com.cleanlearning.domain.user.value;

import java.util.Objects;

/** The name an expert shows publicly, from their expert profile. It is never blank. */
public record DisplayName(String value) {

  public DisplayName {
    Objects.requireNonNull(value, "value");
    if (isBlank(value)) {
      throw new IllegalArgumentException("A display name is not blank");
    }
  }

  /**
   * Blank means only whitespace, spaces (no-break spaces included) and U+0085, which is what {@code
   * \s} matches in Postgres under the column's ICU collation, so this is the rule of the check
   * constraint on {@code expert_profile.display_name}. {@link String#isBlank()} misses the no-break
   * spaces and U+0085.
   */
  private static boolean isBlank(String value) {
    return value
        .codePoints()
        .allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 0x85);
  }
}
