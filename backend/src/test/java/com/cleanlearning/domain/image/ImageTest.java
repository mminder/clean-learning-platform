package com.cleanlearning.domain.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.cleanlearning.TestImages;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.image.value.ImageType;
import com.cleanlearning.domain.shared.InvalidValueException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImageTest {

  @Test
  void anUploadedFileGetsANewIdAndTheTypeOfItsFirstBytes() {
    byte[] png = TestImages.png(100);

    Image image = Image.upload(png);

    assertThat(image.type()).isEqualTo(ImageType.PNG);
    assertThat(image.bytes()).isEqualTo(png);
    assertThat(Image.upload(png).id()).isNotEqualTo(image.id());
  }

  @Test
  void mayHaveTwoMegabytes() {
    assertThat(Image.upload(TestImages.jpeg(Image.MAX_BYTES)).bytes()).hasSize(2 * 1024 * 1024);
  }

  @Test
  void rejectsMoreThanTwoMegabytes() {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> Image.upload(TestImages.jpeg(Image.MAX_BYTES + 1)))
        .withMessage("An image is at most 2 MB.");
  }

  @Test
  void rejectsAnEmptyFile() {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> Image.upload(new byte[0]))
        .withMessage("An image is not empty.");
  }

  @Test
  void rejectsAFileOfAnotherType() {
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> Image.upload(TestImages.gif(100)))
        .withMessage("An image is a JPEG, PNG or WebP file.");
  }

  @Test
  void aStoredImageKeepsTheSizeRules() {
    ImageId id = new ImageId(UUID.randomUUID());
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new Image(id, ImageType.PNG, new byte[0]));
    assertThatExceptionOfType(InvalidValueException.class)
        .isThrownBy(() -> new Image(id, ImageType.PNG, TestImages.png(Image.MAX_BYTES + 1)));
  }

  @Test
  void keepsItsBytesToItself() {
    byte[] png = TestImages.png(100);
    Image image = Image.upload(png);

    png[50] = 0;
    image.bytes()[51] = 0;

    assertThat(image.bytes()).isEqualTo(TestImages.png(100));
  }
}
