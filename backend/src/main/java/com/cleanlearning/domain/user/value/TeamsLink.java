package com.cleanlearning.domain.user.value;

import com.cleanlearning.domain.shared.InvalidValueException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An expert's link to their calls (UC-14): {@code https://} followed by at least one printable
 * ASCII character, without spaces. Any host, not only Microsoft's (spec 06, decision 8). The check
 * constraint on {@code expert_profile.teams_link} has the same expression, and the page can use it
 * as a link's address as it is.
 */
public record TeamsLink(String value) {

  /** {@code https://}, then at least one printable ASCII character but the space. */
  public static final String PATTERN = "https://[!-~]+";

  private static final Pattern HTTPS_LINK = Pattern.compile(PATTERN);

  public TeamsLink {
    Objects.requireNonNull(value, "value");
    if (!HTTPS_LINK.matcher(value).matches()) {
      throw new InvalidValueException("A Teams link is an https:// link.");
    }
  }
}
