package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The check constraints of {@code V5__create_image.sql} repeat the rules of {@code Image} and {@code ImageType}
 * (D-36): a JPEG, PNG or WebP content type, and 1 byte to 2 MB.
 */
@IntegrationTest
class ImageCheckConstraintsTest {

    @Autowired
    JdbcClient jdbc;

    @AfterEach
    void deleteImages() {
        jdbc.sql("delete from image where not exists (select 1 from offering o where o.image_id = image.id)").update();
    }

    private void insert(String contentType, byte[] data) {
        jdbc.sql("insert into image (id, content_type, data) values (?, ?, ?)")
                .params(UUID.randomUUID(), contentType, data)
                .update();
    }

    @Test
    void acceptsTheImageTypesFromOneByteToTwoMegabytes() {
        assertThatCode(() -> insert("image/jpeg", new byte[1])).doesNotThrowAnyException();
        assertThatCode(() -> insert("image/png", new byte[TestImages.MAX_BYTES])).doesNotThrowAnyException();
        assertThatCode(() -> insert("image/webp", new byte[1])).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnotherContentType() {
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> insert("image/gif", new byte[1]));
    }

    @Test
    void rejectsAnEmptyImageAndOneOfMoreThanTwoMegabytes() {
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> insert("image/png", new byte[0]));
        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> insert("image/png", new byte[TestImages.MAX_BYTES + 1]));
    }
}
