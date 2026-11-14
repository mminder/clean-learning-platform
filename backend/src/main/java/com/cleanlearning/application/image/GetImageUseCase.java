package com.cleanlearning.application.image;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetImageUseCase {

  private final ImageStorePort imageStore;

  GetImageUseCase(ImageStorePort imageStore) {
    this.imageStore = imageStore;
  }

  @Transactional(readOnly = true)
  public Image invoke(ImageId id) {
    return imageStore
        .load(id)
        .orElseThrow(() -> new NotFoundException("Image " + id.value() + " not found."));
  }
}
