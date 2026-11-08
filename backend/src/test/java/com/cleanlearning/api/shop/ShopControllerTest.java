package com.cleanlearning.api.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.cleanlearning.IntegrationTest;
import com.cleanlearning.SeedData;

/**
 * UC-02 shop list and offering detail. Tests that need more than the seed add rows with {@code @Sql} and remove them
 * afterwards, because all integration tests share one database.
 */
@IntegrationTest
class ShopControllerTest {

    private static final String CAREER_COACHING_DESCRIPTION = "One-to-one sessions with an experienced coach for your "
            + "next career step. Together you take stock of your strengths, sharpen your goals and plan concrete "
            + "actions, whether you are aiming for a new role, a promotion or a change of field. Between sessions you "
            + "try things out, and the next session builds on what you learned.";

    private static final String EXPERT_1_BIO = "Career coach with ten years in tech recruiting. Helps engineers "
            + "find their next role and prepare for interviews.";

    private static final String EXPERT_2_BIO = "Leadership coach and former engineering manager. Works with new "
            + "team leads on their first year in the role.";

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
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void hidesDraftAndArchivedOfferings() {
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql("more-published-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void sortsOfferingsAlphabeticallyByName() {
        // Alphabetically, regardless of umlauts: a byte-wise sort would put "Ärger-Coaching" last.
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Agile Coaching", "Ärger-Coaching", "Career Coaching");
    }

    @Test
    void showsTheSeededConsultationOffering() {
        // Packages come fewest sessions first, experts by display name.
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", SeedData.CAREER_COACHING))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "id": "00000000-0000-0000-0000-000000001001",
                          "type": "CONSULTATION",
                          "name": "Career Coaching",
                          "description": "%s",
                          "consultation": {
                            "sessionDurationMinutes": 60,
                            "cancellationDeadlineHours": 24,
                            "packages": [
                              {
                                "id": "00000000-0000-0000-0000-000000001011",
                                "sessionCount": 3,
                                "price": { "amount": "450.00", "currency": "CHF" }
                              },
                              {
                                "id": "00000000-0000-0000-0000-000000001012",
                                "sessionCount": 5,
                                "price": { "amount": "700.00", "currency": "CHF" }
                              }
                            ],
                            "eligibleExperts": [
                              {
                                "id": "00000000-0000-0000-0000-000000000201",
                                "displayName": "Expert 1",
                                "bio": "%s"
                              },
                              {
                                "id": "00000000-0000-0000-0000-000000000202",
                                "displayName": "Expert 2",
                                "bio": "%s"
                              }
                            ]
                          }
                        }
                        """.formatted(CAREER_COACHING_DESCRIPTION, EXPERT_1_BIO, EXPERT_2_BIO));
    }

    @Test
    @Sql("more-experts-and-packages.sql")
    @Sql(scripts = "delete-more-experts-and-packages.sql", executionPhase = AFTER_TEST_METHOD)
    void sortsPackagesBySessionCountAndExpertsAlphabeticallyByDisplayName() {
        var body = assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", SeedData.CAREER_COACHING))
                .hasStatusOk()
                .bodyJson();
        body.extractingPath("$.consultation.packages[*].sessionCount").asArray().containsExactly(1, 3, 5);
        body.extractingPath("$.consultation.eligibleExperts[*].displayName").asArray()
                .containsExactly("Älma Muster", "anna Beispiel", "Expert 1", "Expert 2");
    }

    @Test
    void answersUnknownOfferingsWithNotFound() {
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", "00000000-0000-0000-0000-000000000000"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "status": 404,
                          "title": "Not Found",
                          "instance": "/api/shop/offerings/00000000-0000-0000-0000-000000000000"
                        }
                        """);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "00000000-0000-0000-0000-000000009001", "00000000-0000-0000-0000-000000009002" })
    @Sql("draft-and-archived-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersDraftAndArchivedOfferingsWithNotFound(String draftOrArchivedId) {
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", draftOrArchivedId))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(404);
    }

    @Test
    @Sql("published-offering-without-packages.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void hidesAPublishedOfferingWithoutPackages() {
        // UC-16 allows removing the last package of a published offering; the shop then hides it until one is added.
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", "00000000-0000-0000-0000-000000009005"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "status": 404,
                          "title": "Not Found",
                          "instance": "/api/shop/offerings/00000000-0000-0000-0000-000000009005"
                        }
                        """);
    }

    @Test
    void answersAMalformedIdWithBadRequest() {
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", "not-a-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "status": 400,
                          "title": "Bad Request",
                          "instance": "/api/shop/offerings/not-a-uuid"
                        }
                        """);
    }

    @Test
    void answersUnknownShopRoutesWithNotFound() {
        assertThat(mockMvc.get().uri("/api/shop/nope"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(404);
    }
}
