package com.cleanlearning.application.user;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.application.user.port.LoadOwnExpertProfilePort;
import com.cleanlearning.application.user.port.LockExpertPort;
import com.cleanlearning.application.user.port.SaveOwnExpertProfilePort;
import com.cleanlearning.domain.image.Image;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.OwnExpertProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A replaced photo is deleted, and the new one has a new id, so its URL can be cached for good
 * (D-21).
 */
@Service
public class UploadExpertPhotoUseCase {

  private final LockExpertPort lockExpert;

  private final LoadOwnExpertProfilePort loadOwnExpertProfile;

  private final SaveOwnExpertProfilePort saveOwnExpertProfile;

  private final ImageStorePort imageStore;

  UploadExpertPhotoUseCase(
      LockExpertPort lockExpert,
      LoadOwnExpertProfilePort loadOwnExpertProfile,
      SaveOwnExpertProfilePort saveOwnExpertProfile,
      ImageStorePort imageStore) {
    this.lockExpert = lockExpert;
    this.loadOwnExpertProfile = loadOwnExpertProfile;
    this.saveOwnExpertProfile = saveOwnExpertProfile;
    this.imageStore = imageStore;
  }

  /**
   * Uploads or replaces the current user's photo (UC-14). Locks the expert first, so two
   * replacements can't leave an image behind (spec 06, decision 13; D-27).
   */
  @Transactional
  public OwnExpertProfile invoke(CurrentUser user, byte[] file) {
    lockExpert.lock(user.id());
    OwnExpertProfile profile = loadOwnExpertProfile.load(user.id());
    Image image = Image.upload(file);
    OwnExpertProfile changed = profile.changePhoto(image.id());
    imageStore.save(image);
    saveOwnExpertProfile.save(changed);
    profile.profile().photoId().ifPresent(imageStore::delete);
    return changed;
  }
}
