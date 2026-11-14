package com.cleanlearning.domain.shared;

/**
 * A broken invariant or a programming error is not this: it is an {@link IllegalStateException}, a
 * 500.
 */
public class InvalidValueException extends RuntimeException {

  public InvalidValueException(String message) {
    super(message);
  }

  public InvalidValueException(String message, Throwable cause) {
    super(message, cause);
  }
}
