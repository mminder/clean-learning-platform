package com.cleanlearning.domain.shared;

/**
 * The resource doesn't exist, or the caller may not see it. The API answers 404 either way, so
 * nobody can probe which ids exist.
 */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
