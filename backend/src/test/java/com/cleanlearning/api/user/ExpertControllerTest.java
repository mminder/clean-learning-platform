package com.cleanlearning.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.domain.user.value.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@code GET /api/admin/experts} (UC-16, eligible experts): the experts an admin can choose from.
 */
@IntegrationTest
class ExpertControllerTest {

  private static final String SCRIPTS = "/com/cleanlearning/api/user/";

  @Autowired MockMvcTester mockMvc;

  private MockMvcTester.MockMvcRequestBuilder listExperts() {
    return mockMvc.get().uri("/api/admin/experts").with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN));
  }

  @Test
  void listsTheExpertsByDisplayNameWithoutCustomersAndAdmins() {
    // Expert 2 is an admin, too, and Expert 1 a customer; Customer 1 to 2 and Admin 1 to 2 are no
    // experts.
    assertThat(listExperts())
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            [
              { "id": "00000000-0000-0000-0000-000000000201", "displayName": "Expert 1" },
              { "id": "00000000-0000-0000-0000-000000000202", "displayName": "Expert 2" }
            ]
            """);
  }

  @Test
  @Sql(SCRIPTS + "users-that-are-not-offered-as-experts.sql")
  @Sql(
      scripts = SCRIPTS + "delete-users-that-are-not-offered-as-experts.sql",
      executionPhase = AFTER_TEST_METHOD)
  void leavesOutAnExpertWithoutProfileAndAProfileWithoutTheRole() {
    assertThat(listExperts())
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].displayName")
        .asArray()
        .containsExactly("Expert 1", "Expert 2");
  }
}
