package com.cleanlearning.application.image;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.shared.NotFoundException;

/**
 * Serves an image to everyone (D-21): an offering's image in the shop and the admin's page, later an expert's photo.
 */
@Service
public class GetImageUseCase {

    private final ImageStorePort imageStore;

    GetImageUseCase(ImageStorePort imageStore) {
        this.imageStore = imageStore;
    }

    /**
     * @throws NotFoundException if there's no image with this id, such as one that was replaced
     */
    @Transactional(readOnly = true)
    public Image invoke(ImageId id) {
        return imageStore.load(id).orElseThrow(() -> new NotFoundException("Image " + id.value() + " not found."));
    }
}
