package com.cleanlearning.domain.shared;

/**
 * Also when the caller may not see the resource: the API answers 404 either way, so nobody can
 * probe which ids exist.
 */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}
