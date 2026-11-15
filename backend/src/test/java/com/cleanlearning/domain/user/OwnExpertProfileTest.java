package com.cleanlearning.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import com.cleanlearning.domain.image.value.ImageId;
import com.cleanlearning.domain.user.value.DisplayName;
import com.cleanlearning.domain.user.value.TeamsLink;
import com.cleanlearning.domain.user.value.UserId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OwnExpertProfileTest {

  private static final UserId EXPERT_2 =
      new UserId(UUID.fromString("00000000-0000-0000-0000-000000000202"));

  private static final OwnExpertProfile SEEDED =
      new OwnExpertProfile(
          new ExpertProfile(
              EXPERT_2, new DisplayName("Expert 2"), "Leadership coach.", Optional.empty()),
          Optional.empty());

  private static final ImageId PHOTO =
      new ImageId(UUID.fromString("00000000-0000-0000-0000-000000009001"));

  @Test
  void anEditReplacesTheNameTheBioAndTheLinkAndKeepsTheIdAndThePhoto() {
    TeamsLink link = new TeamsLink("https://example.com/meet/expert-2");

    OwnExpertProfile edited =
        SEEDED.changePhoto(PHOTO).edit(new DisplayName("Expert Two"), "New bio", Optional.of(link));

    assertThat(edited)
        .isEqualTo(
            new OwnExpertProfile(
                new ExpertProfile(
                    EXPERT_2, new DisplayName("Expert Two"), "New bio", Optional.of(PHOTO)),
                Optional.of(link)));
  }

  @Test
  void anEditWithoutALinkRemovesItAndAnEmptyBioStaysEmpty() {
    OwnExpertProfile withLink =
        SEEDED.edit(
            new DisplayName("Expert 2"),
            "Leadership coach.",
            Optional.of(new TeamsLink("https://example.com/meet/expert-2")));

    OwnExpertProfile edited = withLink.edit(new DisplayName("Expert 2"), "", Optional.empty());

    assertThat(edited.teamsLink()).isEmpty();
    assertThat(edited.profile().bio()).isEmpty();
    assertThat(edited.profile().id()).isEqualTo(EXPERT_2);
  }

  @Test
  void aChangedPhotoPointsAtTheNewImageAndKeepsTheRest() {
    TeamsLink link = new TeamsLink("https://example.com/meet/expert-2");
    OwnExpertProfile withLink = SEEDED.edit(new DisplayName("Expert 2"), "Bio", Optional.of(link));
    ImageId newPhoto = new ImageId(UUID.fromString("00000000-0000-0000-0000-000000009002"));

    OwnExpertProfile changed = withLink.changePhoto(PHOTO).changePhoto(newPhoto);

    assertThat(changed)
        .isEqualTo(
            new OwnExpertProfile(
                new ExpertProfile(
                    EXPERT_2, new DisplayName("Expert 2"), "Bio", Optional.of(newPhoto)),
                Optional.of(link)));
  }

  @Test
  void aRemovedPhotoIsNoneAndTheRestStays() {
    TeamsLink link = new TeamsLink("https://example.com/meet/expert-2");
    OwnExpertProfile withPhoto =
        SEEDED.edit(new DisplayName("Expert 2"), "Bio", Optional.of(link)).changePhoto(PHOTO);

    OwnExpertProfile removed = withPhoto.removePhoto();

    assertThat(removed)
        .isEqualTo(
            new OwnExpertProfile(
                new ExpertProfile(EXPERT_2, new DisplayName("Expert 2"), "Bio", Optional.empty()),
                Optional.of(link)));
    assertThat(removed.removePhoto()).isEqualTo(removed);
  }

  @Test
  void needsAllItsValues() {
    assertThatNullPointerException().isThrownBy(() -> new OwnExpertProfile(null, Optional.empty()));
    assertThatNullPointerException().isThrownBy(() -> new OwnExpertProfile(SEEDED.profile(), null));
  }
}
