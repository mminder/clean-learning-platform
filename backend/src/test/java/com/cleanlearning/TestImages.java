package com.cleanlearning;

import com.cleanlearning.domain.image.Image;
import java.util.Arrays;

/**
 * Image files for tests. Only their first bytes make them a type (D-21), so each is its signature,
 * padded to a size.
 */
public final class TestImages {

  /** 2 MB, the most an image may have (D-21). */
  public static final int MAX_BYTES = Image.MAX_BYTES;

  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

  private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

  private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

  private static final byte[] GIF = {'G', 'I', 'F', '8', '9', 'a'};

  private TestImages() {}

  public static byte[] jpeg(int size) {
    return padded(JPEG, size);
  }

  public static byte[] png(int size) {
    return padded(PNG, size);
  }

  public static byte[] webp(int size) {
    return padded(WEBP, size);
  }

  public static byte[] gif(int size) {
    return padded(GIF, size);
  }

  /**
   * The signature, then the byte {@code 7} up to the size, so two images of one type and size are
   * equal. A size below the signature's length cuts the signature off.
   */
  private static byte[] padded(byte[] signature, int size) {
    byte[] bytes = Arrays.copyOf(signature, size);
    if (size > signature.length) {
      Arrays.fill(bytes, signature.length, size, (byte) 7);
    }
    return bytes;
  }
}
