package com.cleanlearning.api.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.domain.user.value.Role;

/**
 * UC-16 step 1, the admin's list. Tests that need more than the seed add rows with {@code @Sql} and remove them
 * afterwards, because all integration tests share one database. They reuse the shop tests' scripts.
 */
@IntegrationTest
class ConsultationOfferingControllerTest {

    private static final String SHOP = "/com/cleanlearning/api/shop/";

    @Autowired
    MockMvcTester mockMvc;

    private MockMvcTester.MockMvcRequestBuilder list() {
        return mockMvc.get().uri("/api/admin/consultation-offerings").with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN));
    }

    @Test
    void listsTheSeededOfferingWithItsCounts() {
        assertThat(list())
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          {
                            "id": "00000000-0000-0000-0000-000000001001",
                            "name": "Career Coaching",
                            "status": "PUBLISHED",
                            "packageCount": 2,
                            "eligibleExpertCount": 2
                          }
                        ]
                        """);
    }

    @Test
    @Sql(SHOP + "draft-and-archived-offerings.sql")
    @Sql(scripts = SHOP + "delete-extra-offerings.sql", executionPhase = AFTER_TEST_METHOD)
    void listsEveryStatusSortedByName() {
        assertThat(list())
                .bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Archived Coaching", "Career Coaching", "Draft Coaching");
        assertThat(list())
                .bodyJson().extractingPath("$[*].status").asArray()
                .containsExactly("ARCHIVED", "PUBLISHED", "DRAFT");
    }

    @Test
    @Sql(SHOP + "more-published-offerings.sql")
    @Sql(scripts = SHOP + "delete-extra-offerings.sql", executionPhase = AFTER_TEST_METHOD)
    void sortsAlphabeticallyRegardlessOfUmlauts() {
        assertThat(list())
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Agile Coaching", "Ärger-Coaching", "Career Coaching");
    }

    @Test
    @Sql(SHOP + "more-experts-and-packages.sql")
    @Sql(scripts = SHOP + "delete-more-experts-and-packages.sql", executionPhase = AFTER_TEST_METHOD)
    void countsPackagesAndExpertsWithoutMultiplyingThem() {
        // 3 packages and 4 experts: joining both would give 12 rows.
        assertThat(list())
                .hasStatusOk()
                .bodyJson().extractingPath("$[0].packageCount").isEqualTo(3);
        assertThat(list())
                .bodyJson().extractingPath("$[0].eligibleExpertCount").isEqualTo(4);
    }
}
