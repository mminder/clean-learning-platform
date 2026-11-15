package com.cleanlearning.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

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
          new ExpertProfile(EXPERT_2, new DisplayName("Expert 2"), "Leadership coach."),
          Optional.empty());

  @Test
  void anEditReplacesTheNameTheBioAndTheLinkAndKeepsTheId() {
    TeamsLink link = new TeamsLink("https://example.com/meet/expert-2");

    OwnExpertProfile edited =
        SEEDED.edit(new DisplayName("Expert Two"), "New bio", Optional.of(link));

    assertThat(edited)
        .isEqualTo(
            new OwnExpertProfile(
                new ExpertProfile(EXPERT_2, new DisplayName("Expert Two"), "New bio"),
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
  void needsAllItsValues() {
    assertThatNullPointerException().isThrownBy(() -> new OwnExpertProfile(null, Optional.empty()));
    assertThatNullPointerException().isThrownBy(() -> new OwnExpertProfile(SEEDED.profile(), null));
  }
}
