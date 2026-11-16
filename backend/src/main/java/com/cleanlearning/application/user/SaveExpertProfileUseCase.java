package com.cleanlearning.application.user;

import com.cleanlearning.application.user.port.LoadOwnExpertProfilePort;
import com.cleanlearning.application.user.port.LockExpertPort;
import com.cleanlearning.application.user.port.SaveOwnExpertProfilePort;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.OwnExpertProfile;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.TeamsLink;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaveExpertProfileUseCase {

  private final LockExpertPort lockExpert;

  private final LoadOwnExpertProfilePort loadOwnExpertProfile;

  private final SaveOwnExpertProfilePort saveOwnExpertProfile;

  SaveExpertProfileUseCase(
      LockExpertPort lockExpert,
      LoadOwnExpertProfilePort loadOwnExpertProfile,
      SaveOwnExpertProfilePort saveOwnExpertProfile) {
    this.lockExpert = lockExpert;
    this.loadOwnExpertProfile = loadOwnExpertProfile;
    this.saveOwnExpertProfile = saveOwnExpertProfile;
  }

  /**
   * Saves the current user's own profile (UC-14 step 4). Locks the expert first, as saving the
   * availability does (spec 06, decision 13; D-27).
   */
  @Transactional
  public OwnExpertProfile invoke(CurrentUser user, Command command) {
    lockExpert.lock(user.id());
    OwnExpertProfile profile =
        loadOwnExpertProfile
            .load(user.id())
            .edit(command.displayName(), command.bio(), command.teamsLink());
    saveOwnExpertProfile.save(profile);
    return profile;
  }

  /** The bio may be empty, and an empty {@code teamsLink} removes the link. */
  public record Command(DisplayName displayName, String bio, Optional<TeamsLink> teamsLink) {

    public Command {
      Objects.requireNonNull(displayName, "displayName");
      Objects.requireNonNull(bio, "bio");
      Objects.requireNonNull(teamsLink, "teamsLink");
    }
  }
}
