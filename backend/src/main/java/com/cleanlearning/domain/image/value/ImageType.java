package com.cleanlearning.domain.image.value;

import com.cleanlearning.domain.shared.InvalidValueException;
import java.util.Arrays;

/**
 * Detected from the file's signature, never from its name or the content type the browser declared.
 */
public enum ImageType {
  JPEG("image/jpeg"),
  PNG("image/png"),
  WEBP("image/webp");

  private static final String NOT_AN_IMAGE = "An image is a JPEG, PNG or WebP file.";

  private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

  // A WebP file is a RIFF container: "RIFF", the size in four bytes, then "WEBP".
  private static final byte[] RIFF = {'R', 'I', 'F', 'F'};

  private static final byte[] WEBP_FORM = {'W', 'E', 'B', 'P'};

  private final String contentType;

  ImageType(String contentType) {
    this.contentType = contentType;
  }

  public String contentType() {
    return contentType;
  }

  public static ImageType detect(byte[] bytes) {
    if (startsWith(bytes, 0, JPEG_SIGNATURE)) {
      return JPEG;
    }
    if (startsWith(bytes, 0, PNG_SIGNATURE)) {
      return PNG;
    }
    if (startsWith(bytes, 0, RIFF) && startsWith(bytes, 8, WEBP_FORM)) {
      return WEBP;
    }
    throw new InvalidValueException(NOT_AN_IMAGE);
  }

  public static ImageType ofContentType(String contentType) {
    return Arrays.stream(values())
        .filter(type -> type.contentType.equals(contentType))
        .findFirst()
        .orElseThrow(() ->
            new IllegalArgumentException("No image type has the content type " + contentType));
  }

  private static boolean startsWith(byte[] bytes, int offset, byte[] signature) {
    return bytes.length >= offset + signature.length
        && Arrays.equals(bytes, offset, offset + signature.length, signature, 0, signature.length);
  }
}
