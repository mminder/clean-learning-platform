package com.cleanlearning.application.image.port;

import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.image.value.ImageId;
import java.util.Optional;

/**
 * Where image bytes are kept (D-21): a Postgres table for now, replaceable by an object store
 * later. An image is never changed, only saved and deleted.
 */
public interface ImageStorePort {

  /** Inserts a new image. */
  void save(Image image);

  /** The image, or empty if there's none with this id. */
  Optional<Image> load(ImageId id);

  /** Deletes the image for good. Nothing may refer to it any more when the transaction commits. */
  void delete(ImageId id);
}
