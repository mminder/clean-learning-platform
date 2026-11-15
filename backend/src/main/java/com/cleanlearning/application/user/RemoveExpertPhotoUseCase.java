package com.cleanlearning.application.user;

import com.cleanlearning.application.image.port.ImageStorePort;
import com.cleanlearning.application.user.port.LoadOwnExpertProfilePort;
import com.cleanlearning.application.user.port.LockExpertPort;
import com.cleanlearning.application.user.port.SaveOwnExpertProfilePort;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.OwnExpertProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveExpertPhotoUseCase {

  private final LockExpertPort lockExpert;

  private final LoadOwnExpertProfilePort loadOwnExpertProfile;

  private final SaveOwnExpertProfilePort saveOwnExpertProfile;

  private final ImageStorePort imageStore;

  RemoveExpertPhotoUseCase(
      LockExpertPort lockExpert,
      LoadOwnExpertProfilePort loadOwnExpertProfile,
      SaveOwnExpertProfilePort saveOwnExpertProfile,
      ImageStorePort imageStore) {
    this.lockExpert = lockExpert;
    this.loadOwnExpertProfile = loadOwnExpertProfile;
    this.saveOwnExpertProfile = saveOwnExpertProfile;
    this.imageStore = imageStore;
  }

  /** Removes the current user's photo (UC-14), after locking the expert (D-27). */
  @Transactional
  public OwnExpertProfile invoke(CurrentUser user) {
    lockExpert.lock(user.id());
    OwnExpertProfile profile = loadOwnExpertProfile.load(user.id());
    OwnExpertProfile changed = profile.removePhoto();
    saveOwnExpertProfile.save(changed);
    profile.profile().photoId().ifPresent(imageStore::delete);
    return changed;
  }
}
