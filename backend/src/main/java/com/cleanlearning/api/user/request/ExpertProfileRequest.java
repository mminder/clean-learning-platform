package com.cleanlearning.api.user.request;

import com.cleanlearning.application.user.SaveExpertProfileUseCase;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.TeamsLink;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * The annotations repeat UC-14's rules so a 400 names every invalid field; the value objects stay
 * the authority. The bio may be empty; a {@code teamsLink} of {@code null} or {@code ""} is no
 * link.
 */
public record ExpertProfileRequest(
    @NotBlank String displayName,
    @NotNull String bio,
    @Pattern(regexp = TEAMS_LINK_PATTERN, message = "must be an https:// link")
        @Nullable String teamsLink) {

  public SaveExpertProfileUseCase.Command toCommand() {
    return new SaveExpertProfileUseCase.Command(
        new DisplayName(displayName),
        bio,
        Optional.ofNullable(teamsLink).filter(link -> !link.isEmpty()).map(TeamsLink::new));
  }

  // Bean Validation matches the whole value; the anchors say so in openapi.json, where a pattern
  // matches anywhere. The empty link is no link.
  static final String TEAMS_LINK_PATTERN = "^(" + TeamsLink.PATTERN + ")?$";
}
