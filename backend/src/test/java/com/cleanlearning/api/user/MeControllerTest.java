package com.cleanlearning.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.domain.user.value.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code GET /api/me} (UC-01): the logged-in user, or 401. */
@IntegrationTest
class MeControllerTest {

  @Autowired MockMvcTester mockMvc;

  @Test
  void returnsTheLoggedInUserWithNameEmailAndRolesFromTheDatabase() {
    // The session's roles decide access; the response reads the user's roles fresh.
    assertThat(mockMvc.get().uri("/api/me").with(LoggedIn.as(SeedData.EXPERT_2, Role.ADMIN)))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "id": "00000000-0000-0000-0000-000000000202",
              "name": "Expert 2",
              "email": "expert2@example.com",
              "roles": ["EXPERT", "ADMIN"]
            }
            """);
  }

  @Test
  void answers401WithAnEmptyBodyWithoutALogin() {
    MvcTestResult result = mockMvc.get().uri("/api/me").exchange();

    assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED).body().isEmpty();
  }
}
