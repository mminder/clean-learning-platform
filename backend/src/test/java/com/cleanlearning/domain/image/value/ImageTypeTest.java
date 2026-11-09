package com.cleanlearning.domain.image.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.cleanlearning.TestImages;

class ImageTypeTest {

    @Test
    void detectsJpegPngAndWebpFromTheFirstBytes() {
        assertThat(ImageType.detect(TestImages.jpeg(100))).isEqualTo(ImageType.JPEG);
        assertThat(ImageType.detect(TestImages.png(100))).isEqualTo(ImageType.PNG);
        assertThat(ImageType.detect(TestImages.webp(100))).isEqualTo(ImageType.WEBP);
    }

    @Test
    void detectsTheSmallestFileOfEachType() {
        assertThat(ImageType.detect(TestImages.jpeg(3))).isEqualTo(ImageType.JPEG);
        assertThat(ImageType.detect(TestImages.png(8))).isEqualTo(ImageType.PNG);
        assertThat(ImageType.detect(TestImages.webp(12))).isEqualTo(ImageType.WEBP);
    }

    @Test
    void rejectsAnyOtherFile() {
        String message = "An image is a JPEG, PNG or WebP file.";
        assertThatIllegalArgumentException().isThrownBy(() -> ImageType.detect(TestImages.gif(100)))
                .withMessage(message);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImageType.detect("not a picture".getBytes(StandardCharsets.UTF_8)))
                .withMessage(message);
        // a RIFF file that isn't WebP, such as a WAV
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ImageType.detect("RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII)))
                .withMessage(message);
        assertThatIllegalArgumentException().isThrownBy(() -> ImageType.detect(new byte[0])).withMessage(message);
    }

    @Test
    void rejectsACutOffSignature() {
        assertThatIllegalArgumentException().isThrownBy(() -> ImageType.detect(TestImages.jpeg(2)));
        assertThatIllegalArgumentException().isThrownBy(() -> ImageType.detect(TestImages.png(7)));
        assertThatIllegalArgumentException().isThrownBy(() -> ImageType.detect(TestImages.webp(11)));
    }

    @Test
    void hasItsContentType() {
        assertThat(ImageType.JPEG.contentType()).isEqualTo("image/jpeg");
        assertThat(ImageType.PNG.contentType()).isEqualTo("image/png");
        assertThat(ImageType.WEBP.contentType()).isEqualTo("image/webp");
    }

    @Test
    void isFoundByItsContentType() {
        assertThat(ImageType.ofContentType("image/webp")).isEqualTo(ImageType.WEBP);
        assertThatIllegalArgumentException().isThrownBy(() -> ImageType.ofContentType("image/gif"));
    }
}
