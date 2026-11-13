package com.cleanlearning.api.image;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.application.image.GetImageUseCase;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.image.value.ImageId;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Images for everyone (D-21): an offering's image, later an expert's photo. An image never changes,
 * a replaced one gets a new id, so browsers may cache it for a year without asking again. The
 * header is set here, so Spring Security's {@code no-cache} headers stay off this response.
 */
@RestController
@RequestMapping("/api/images")
@PreAuthorize("permitAll()")
class ImageController {

  static final String CACHE_FOR_GOOD = "public, max-age=31536000, immutable";

  private final GetImageUseCase getImage;

  ImageController(GetImageUseCase getImage) {
    this.getImage = getImage;
  }

  /** A {@link Resource}, so {@code openapi.json} says the body is binary, not base64. */
  @GetMapping(
      path = "/{id}",
      produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"})
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status404
  ResponseEntity<Resource> getImage(@PathVariable UUID id) {
    Image image = getImage.invoke(new ImageId(id));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.type().contentType()))
        .header(HttpHeaders.CACHE_CONTROL, CACHE_FOR_GOOD)
        .body(new ByteArrayResource(image.bytes()));
  }
}
