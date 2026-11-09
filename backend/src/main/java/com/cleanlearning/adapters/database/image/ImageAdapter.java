package com.cleanlearning.adapters.database.image;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cleanlearning.adapters.database.image.entity.ImageEntity;
import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.image.value.ImageType;

import jakarta.persistence.EntityManager;

/**
 * The images in the {@code image} table (D-21). The application sets the id, so a new image is persisted, not merged.
 */
@Component
class ImageAdapter implements ImageStorePort {

    private final EntityManager entityManager;

    ImageAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void save(Image image) {
        entityManager.persist(new ImageEntity(image.id().value(), image.type().contentType(), image.bytes()));
    }

    @Override
    public Optional<Image> load(ImageId id) {
        return Optional.ofNullable(entityManager.find(ImageEntity.class, id.value())).map(ImageAdapter::toImage);
    }

    /**
     * Removes the managed entity, which the transaction deletes when it commits, after the inserts and updates and
     * after the deletes removed before it. So an offering that pointed at the image points elsewhere by then, or is
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
        return new Image(new ImageId(entity.getId()), ImageType.ofContentType(entity.getContentType()),
                entity.getData());
    }
}
