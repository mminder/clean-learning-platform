package com.cleanlearning.api.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.cleanlearning.IntegrationTest;

/**
 * UC-02 shop list. Tests that need more than the seed add rows with {@code @Sql} and remove them afterwards, because
 * all integration tests share one database.
 */
@IntegrationTest
class ShopControllerTest {

    private static final String CAREER_COACHING_DESCRIPTION = "One-to-one sessions with an experienced coach for your "
            + "next career step. Together you take stock of your strengths, sharpen your goals and plan concrete "
            + "actions, whether you are aiming for a new role, a promotion or a change of field. Between sessions you "
            + "try things out, and the next session builds on what you learned.";

    @Autowired
    MockMvcTester mockMvc;

    @Test
    void listsTheSeededOffering() {
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          {
                            "id": "00000000-0000-0000-0000-000000001001",
                            "type": "CONSULTATION",
                            "name": "Career Coaching",
                            "description": "%s",
                            "price": { "amount": "450.00", "currency": "CHF" }
                          }
                        ]
                        """.formatted(CAREER_COACHING_DESCRIPTION));
    }

    @Test
    @Sql("draft-and-archived-offerings.sql")
    @Sql(scripts = "delete-extra-offerings.sql", executionPhase = AFTER_TEST_METHOD)
    void hidesDraftAndArchivedOfferings() {
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql("more-published-offerings.sql")
    @Sql(scripts = "delete-extra-offerings.sql", executionPhase = AFTER_TEST_METHOD)
    void sortsOfferingsAlphabeticallyByName() {
        // Alphabetically, regardless of umlauts: a byte-wise sort would put "Ärger-Coaching" last.
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Agile Coaching", "Ärger-Coaching", "Career Coaching");
    }
}
