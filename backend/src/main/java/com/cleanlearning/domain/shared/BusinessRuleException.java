package com.cleanlearning.domain.shared;

import java.util.Objects;

/**
 * The code is stable: code that reacts to a conflict, and tests, rely on it; the message may change
 * (D-34).
 */
public class BusinessRuleException extends RuntimeException {

  private final String code;

  public BusinessRuleException(String code, String message) {
    super(message);
    this.code = Objects.requireNonNull(code, "code");
  }

  public String code() {
    return code;
  }
}
