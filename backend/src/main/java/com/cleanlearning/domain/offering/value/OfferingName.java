package com.cleanlearning.domain.offering.value;

import com.cleanlearning.domain.shared.InvalidValueException;
import java.util.Objects;

public record OfferingName(String value) {

  public OfferingName {
    Objects.requireNonNull(value, "value");
    if (isBlank(value)) {
      throw new InvalidValueException("An offering name is not blank");
    }
  }

  /**
   * Blank means only whitespace, no-break spaces and U+0085: what {@code \s} matches in Postgres
   * under the column's ICU collation, so the rule of the check constraint on {@code offering.name}.
   * {@link String#isBlank()} misses the no-break spaces and U+0085.
   */
  private static boolean isBlank(String value) {
    return value
        .codePoints()
        .allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || c == 0x85);
  }
}
