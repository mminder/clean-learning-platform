package com.cleanlearning.domain.image;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.image.value.ImageType;
import com.cleanlearning.domain.shared.InvalidValueException;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable: a replaced image is a new image with a new id, so it can be cached for good. The bytes
 * are copied in and out.
 */
public record Image(ImageId id, ImageType type, byte[] bytes) {

  /** Matches the check constraint on {@code image.data}. */
  public static final int MAX_BYTES = 2 * 1024 * 1024;

  public Image {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(type, "type");
    requireSize(bytes);
    bytes = bytes.clone();
  }

  public static Image upload(byte[] bytes) {
    // The size comes first: an empty file has no type either, but "not empty" says what's wrong.
    requireSize(bytes);
    return new Image(new ImageId(UUID.randomUUID()), ImageType.detect(bytes), bytes);
  }

  @Override
  public byte[] bytes() {
    return bytes.clone();
  }

  private static void requireSize(byte[] bytes) {
    if (bytes.length == 0) {
      throw new InvalidValueException("An image is not empty.");
    }
    if (bytes.length > MAX_BYTES) {
      throw new InvalidValueException("An image is at most 2 MB.");
    }
  }
}
