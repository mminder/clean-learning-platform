package com.cleanlearning.api.user;

import com.cleanlearning.api.ProblemResponses;
import com.cleanlearning.api.user.request.ExpertProfileRequest;
import com.cleanlearning.api.user.response.ExpertProfileResponse;
import com.cleanlearning.application.user.GetExpertProfileUseCase;
import com.cleanlearning.application.user.SaveExpertProfileCommand;
import com.cleanlearning.application.user.SaveExpertProfileUseCase;
import com.cleanlearning.domain.user.CurrentUser;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.TeamsLink;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** UC-14. The routes have no id: the profile is always the current user's. */
@RestController
@RequestMapping(ExpertProfileController.PATH)
@PreAuthorize("hasRole('EXPERT')")
class ExpertProfileController {

  static final String PATH = "/api/expert/profile";

  private final GetExpertProfileUseCase getExpertProfile;

  private final SaveExpertProfileUseCase saveExpertProfile;

  ExpertProfileController(
      GetExpertProfileUseCase getExpertProfile, SaveExpertProfileUseCase saveExpertProfile) {
    this.getExpertProfile = getExpertProfile;
    this.saveExpertProfile = saveExpertProfile;
  }

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  ExpertProfileResponse getExpertProfile(@AuthenticationPrincipal CurrentUser currentUser) {
    return ExpertProfileResponse.from(getExpertProfile.invoke(currentUser));
  }

  @PutMapping
  @ResponseStatus(HttpStatus.OK)
  @ProblemResponses.Status400
  @ProblemResponses.Status415
  ExpertProfileResponse saveExpertProfile(
      @AuthenticationPrincipal CurrentUser currentUser,
      @Valid @RequestBody ExpertProfileRequest request) {
    return ExpertProfileResponse.from(saveExpertProfile.invoke(currentUser, toCommand(request)));
  }

  /** A {@code teamsLink} of {@code null} or {@code ""} is no link. */
  private static SaveExpertProfileCommand toCommand(ExpertProfileRequest request) {
    return new SaveExpertProfileCommand(
        new DisplayName(request.displayName()),
        request.bio(),
        Optional.ofNullable(request.teamsLink())
            .filter(link -> !link.isEmpty())
            .map(TeamsLink::new));
  }
}
