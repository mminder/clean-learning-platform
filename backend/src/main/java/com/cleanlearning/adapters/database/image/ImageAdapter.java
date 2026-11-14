package com.cleanlearning.adapters.database.image;

import com.cleanlearning.adapters.database.image.entity.ImageEntity;
import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.image.value.ImageType;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** The application sets the id, so a new image is persisted, not merged. */
@Component
class ImageAdapter implements ImageStorePort {

  private final EntityManager entityManager;

  ImageAdapter(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public void save(Image image) {
    entityManager.persist(
        new ImageEntity(image.id().value(), image.type().contentType(), image.bytes()));
  }

  @Override
  public Optional<Image> load(ImageId id) {
    return Optional.ofNullable(entityManager.find(ImageEntity.class, id.value()))
        .map(ImageAdapter::toImage);
  }

  /**
   * The transaction deletes the entity when it commits, after the inserts and updates and after
   * earlier deletes, so an offering that pointed at the image points elsewhere by then, or is
   * deleted first.
   */
  @Override
  public void delete(ImageId id) {
    ImageEntity entity = entityManager.find(ImageEntity.class, id.value());
    if (entity == null) {
      throw new IllegalStateException("Image " + id.value() + " doesn't exist.");
    }
    entityManager.remove(entity);
  }

  private static Image toImage(ImageEntity entity) {
    return new Image(
        new ImageId(entity.getId()),
        ImageType.ofContentType(entity.getContentType()),
        entity.getData());
  }
}
