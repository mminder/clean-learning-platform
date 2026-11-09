package com.cleanlearning.domain.image;

import java.util.Objects;
import java.util.UUID;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.image.value.ImageType;

/**
 * An uploaded image (D-21): a JPEG, PNG or WebP file of at most 2 MB, never empty. It is never changed: a replaced
 * image is a new image with a new id, so it can be cached for good. The bytes are copied in and out, so nobody can
 * change them.
 */
public record Image(ImageId id, ImageType type, byte[] bytes) {

    /** 2 MB, 2 × 1024 × 1024 bytes, as the check constraint on {@code image.data} says. */
    public static final int MAX_BYTES = 2 * 1024 * 1024;

    /**
     * @throws IllegalArgumentException if the image is empty or larger than 2 MB
     */
    public Image {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        requireSize(bytes);
        bytes = bytes.clone();
    }

    /**
     * An uploaded file as a new image, with a new id and the type its first bytes say.
     *
     * @throws IllegalArgumentException if the file is empty, larger than 2 MB, or not a JPEG, PNG or WebP file
     */
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
            throw new IllegalArgumentException("An image is not empty.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new IllegalArgumentException("An image is at most 2 MB.");
        }
    }
}
