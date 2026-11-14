package com.cleanlearning.application.image.port;

import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.image.value.ImageId;
import java.util.Optional;

/** A Postgres table for now, replaceable by an object store later. */
public interface ImageStorePort {

  void save(Image image);

  Optional<Image> load(ImageId id);

  /** Nothing may refer to it any more when the transaction commits. */
  void delete(ImageId id);
}
