package com.cleanlearning.api.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestClock;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * UC-14: an expert reads and saves their own profile, and the shop, the expert picker and the
 * customer's case show the new name and bio, but never the Teams link. Tests change the seeded
 * profiles through the API; {@link SeedData#DELETE_TEST_DATA} sets them back afterwards.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ExpertProfileControllerTest {

  private static final String PATH = "/api/expert/profile";

  private static final String CUSTOMER_CASES = "/api/customer/consultation-cases";

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  private static final String TEAMS_LINK = "https://example.com/meet/expert-2";

  private static final String NEW_BIO = "Leadership coach for new team leads.";

  private static final String EXPERT_TWO =
      """
      {
        "displayName": "Expert Two",
        "bio": "Leadership coach for new team leads.",
        "teamsLink": "https://example.com/meet/expert-2"
      }
      """;

  private static final String NOT_HTTPS = "must be an https:// link";

  @Autowired MockMvcTester mockMvc;

  @Autowired TestClock clock;

  @Autowired JdbcClient jdbc;

  @Autowired DataSource dataSource;

  private MvcTestResult save(UUID expert, String body) {
    return mockMvc
        .put()
        .uri(PATH)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .with(Csrf.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private MvcTestResult get(UUID expert) {
    return mockMvc.get().uri(PATH).with(LoggedIn.as(expert, Role.EXPERT)).exchange();
  }

  /** The stored display name, bio and Teams link, joined with " | ". */
  private String stored(UUID expert) {
    return jdbc.sql(
            "select display_name || ' | ' || bio || ' | ' || coalesce(teams_link, 'no link')"
                + " from expert_profile where user_id = ?")
        .param(expert)
        .query(String.class)
        .single();
  }

  private static String profile(String displayName, String bio, String teamsLink) {
    return """
    { "displayName": %s, "bio": %s, "teamsLink": %s }
    """
        .formatted(displayName, bio, teamsLink);
  }

  private static String json(String value) {
    return "\"" + value + "\"";
  }

  /** Expert 2 as the shop, the expert picker and the customer's case show them after the save. */
  private static Map<String, @Nullable Object> expertTwo() {
    Map<String, @Nullable Object> expert = new HashMap<>();
    expert.put("id", SeedData.EXPERT_2.toString());
    expert.put("displayName", "Expert Two");
    expert.put("bio", NEW_BIO);
    expert.put("photoId", null);
    return expert;
  }

  private static Map<String, String> error(String field, String message) {
    return Map.of("field", field, "message", message);
  }

  /** A 400 with these invalid fields, and Expert 2's profile as it was. */
  private void assertRefused(String body, Map<?, ?>... errors) {
    String before = stored(SeedData.EXPERT_2);
    assertThat(save(SeedData.EXPERT_2, body))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.errors")
        .isEqualTo(List.of(errors));
    assertThat(stored(SeedData.EXPERT_2)).isEqualTo(before);
  }

  @Test
  void savesTheProfileAndAnswersIt() {
    String expected =
        """
        {
          "displayName": "Expert Two",
          "bio": "Leadership coach for new team leads.",
          "teamsLink": "https://example.com/meet/expert-2",
          "photoId": null
        }
        """;

    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(expected);
    assertThat(get(SeedData.EXPERT_2)).hasStatusOk().bodyJson().isStrictlyEqualTo(expected);
  }

  @Test
  void answersTheSeededProfileWithoutALink() {
    assertThat(get(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "displayName": "Expert 2",
              "bio": "Leadership coach and former engineering manager. Works with new team leads on their first year in the role.",
              "teamsLink": null,
              "photoId": null
            }
            """);
  }

  @Test
  void savesAnEmptyBioAsEmpty() {
    assertThat(save(SeedData.EXPERT_2, profile(json("Expert 2"), json(""), "null")))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            { "displayName": "Expert 2", "bio": "", "teamsLink": null, "photoId": null }
            """);
    assertThat(stored(SeedData.EXPERT_2)).isEqualTo("Expert 2 |  | no link");
  }

  @Test
  void anEmptyOrMissingLinkRemovesIt() {
    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();
    assertThat(save(SeedData.EXPERT_2, profile(json("Expert Two"), json(NEW_BIO), json(""))))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.teamsLink")
        .isNull();
    assertThat(stored(SeedData.EXPERT_2)).endsWith("| no link");

    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();
    assertThat(save(SeedData.EXPERT_2, profile(json("Expert Two"), json(NEW_BIO), "null")))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.teamsLink")
        .isNull();
    assertThat(stored(SeedData.EXPERT_2)).endsWith("| no link");

    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();
    assertThat(
            save(
                SeedData.EXPERT_2,
                """
                { "displayName": "Expert Two", "bio": "%s" }
                """
                    .formatted(NEW_BIO)))
        .hasStatusOk();
    assertThat(get(SeedData.EXPERT_2)).bodyJson().extractingPath("$.teamsLink").isNull();
  }

  @Test
  void refusesABlankDisplayNameAndALinkThatIsNotHttpsAtTheirFields() {
    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();

    assertThat(
            save(SeedData.EXPERT_2, profile(json(" "), json(NEW_BIO), json("http://example.com"))))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Invalid request content.",
              "instance": "/api/expert/profile",
              "errors": [
                { "field": "displayName", "message": "must not be blank" },
                { "field": "teamsLink", "message": "must be an https:// link" }
              ]
            }
            """);
    assertRefused(
        profile(json(""), json(NEW_BIO), json(TEAMS_LINK)),
        error("displayName", "must not be blank"));
    assertRefused(
        profile(json("Expert Two"), json(NEW_BIO), json("example.com")),
        error("teamsLink", NOT_HTTPS));
    assertRefused(
        profile(json("Expert Two"), json(NEW_BIO), json("https://example.com/meet expert-2")),
        error("teamsLink", NOT_HTTPS));
    assertRefused(
        profile(json("Expert Two"), json(NEW_BIO), json("https://")),
        error("teamsLink", NOT_HTTPS));
    assertRefused(
        "{}", error("bio", "must not be null"), error("displayName", "must not be blank"));
    assertThat(stored(SeedData.EXPERT_2)).isEqualTo("Expert Two | " + NEW_BIO + " | " + TEAMS_LINK);
  }

  /** Bean Validation lets a no-break space through; {@code DisplayName} refuses it. */
  @Test
  void refusesADisplayNameOfNoBreakSpaces() {
    String before = stored(SeedData.EXPERT_2);

    assertThat(save(SeedData.EXPERT_2, profile(json(" "), json(NEW_BIO), "null")))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("A display name is not blank");
    assertThat(stored(SeedData.EXPERT_2)).isEqualTo(before);
  }

  @Test
  void answersABodyThatIsNotJsonWithUnsupportedMediaType() {
    String before = stored(SeedData.EXPERT_2);

    assertThat(
            mockMvc
                .put()
                .uri(PATH)
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content(EXPERT_TWO))
        .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(stored(SeedData.EXPERT_2)).isEqualTo(before);
  }

  @Test
  void eachExpertReadsAndSavesOnlyTheirOwnProfile() {
    String expert1Before = stored(SeedData.EXPERT_1);

    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();
    assertThat(stored(SeedData.EXPERT_1)).isEqualTo(expert1Before);
    assertThat(get(SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.displayName")
        .isEqualTo("Expert 1");

    assertThat(
            save(
                SeedData.EXPERT_1,
                profile(json("Expert One"), json("Career coach."), json("https://example.com/1"))))
        .hasStatusOk();
    assertThat(get(SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "displayName": "Expert One",
              "bio": "Career coach.",
              "teamsLink": "https://example.com/1",
              "photoId": null
            }
            """);
    assertThat(stored(SeedData.EXPERT_2)).isEqualTo("Expert Two | " + NEW_BIO + " | " + TEAMS_LINK);
  }

  @Test
  void theShopShowsTheNewNameAndBioWithoutTheLink() {
    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();

    MvcTestResult detail =
        mockMvc.get().uri("/api/shop/offerings/{id}", SeedData.CAREER_COACHING).exchange();
    assertThat(detail)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.consultation.eligibleExperts[1]")
        .isEqualTo(expertTwo());
    assertThat(detail).body().asString().doesNotContain(TEAMS_LINK);
  }

  /**
   * Customer 1 bought Career Coaching and booked Expert 2 before Expert 2 saved the profile: the
   * expert picker, the case and My purchases show the new name at once, and none of them the link.
   */
  @Test
  void theCustomersPagesShowTheNewNameWithoutTheLink() throws Exception {
    clock.set(NOW);
    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/availability")
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "dates": ["2026-11-17"], "timeRanges": [{ "start": "09:00", "end": "12:00" }] }
                    """))
        .hasStatusOk();
    MvcTestResult bought =
        mockMvc
            .post()
            .uri(CUSTOMER_CASES)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .with(Csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                { "offeringId": "%s", "packageId": "%s", "price": "450.00", "sessionCount": 3 }
                """
                    .formatted(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS))
            .exchange();
    assertThat(bought).hasStatus(HttpStatus.CREATED);
    String caseId = JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
    assertThat(
            mockMvc
                .post()
                .uri(CUSTOMER_CASES + "/{id}/appointments", caseId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "expertId": "%s", "start": "2026-11-17T09:00:00Z" }
                    """
                        .formatted(SeedData.EXPERT_2)))
        .hasStatus(HttpStatus.CREATED);

    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();

    MvcTestResult eligibleExperts =
        mockMvc
            .get()
            .uri(CUSTOMER_CASES + "/{id}/eligible-experts", caseId)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .exchange();
    assertThat(eligibleExperts)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[1]")
        .isEqualTo(expertTwo());
    assertThat(eligibleExperts).body().asString().doesNotContain(TEAMS_LINK);

    MvcTestResult consultationCase =
        mockMvc
            .get()
            .uri(CUSTOMER_CASES + "/{id}", caseId)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .exchange();
    assertThat(consultationCase)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.expert")
        .isEqualTo(expertTwo());
    assertThat(consultationCase).body().asString().doesNotContain(TEAMS_LINK);

    MvcTestResult myPurchases =
        mockMvc
            .get()
            .uri(CUSTOMER_CASES)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .exchange();
    assertThat(myPurchases)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].expertDisplayName")
        .isEqualTo("Expert Two");
    assertThat(myPurchases).body().asString().doesNotContain(TEAMS_LINK);
  }

  /** The test cleanup sets both profiles back to the seed, without a link. */
  @Test
  void theTestCleanupRestoresTheSeededProfiles() {
    String expert1 = stored(SeedData.EXPERT_1);
    String expert2 = stored(SeedData.EXPERT_2);
    assertThat(
            save(
                SeedData.EXPERT_1,
                profile(json("Expert One"), json(""), json("https://example.com/1"))))
        .hasStatusOk();
    assertThat(save(SeedData.EXPERT_2, EXPERT_TWO)).hasStatusOk();

    new ResourceDatabasePopulator(new ClassPathResource(SeedData.DELETE_TEST_DATA))
        .execute(dataSource);

    assertThat(stored(SeedData.EXPERT_1)).isEqualTo(expert1).endsWith("| no link");
    assertThat(stored(SeedData.EXPERT_2)).isEqualTo(expert2).endsWith("| no link");
  }
}
