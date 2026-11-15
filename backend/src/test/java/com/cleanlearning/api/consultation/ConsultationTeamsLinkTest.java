package com.cleanlearning.api.consultation;

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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * UC-10 for consultation appointments (D-19): the assigned expert's Teams link, read from their
 * profile when the case is shown. The expert always has it on their case; the customer has it on an
 * upcoming appointment only inside its link window, from one hour before the start until the end
 * (security.md's shaped responses). Tests set the link, save availability, buy and book through the
 * API; {@link SeedData#DELETE_TEST_DATA} removes it all and sets the profiles back afterwards.
 * "Now" is Mon 9 Nov 2026, 15:00 in Zurich, as in spec 06, until a test moves the clock. In
 * November, Zurich is UTC+1: 10:00 in Zurich is 09:00Z.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ConsultationTeamsLinkTest {

  private static final String CUSTOMER_CASES = "/api/customer/consultation-cases";

  private static final String EXPERT_CASES = "/api/expert/consultation-cases";

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Tue 17 Nov 2026, 10:00 and 11:00 in Zurich: the appointment's start and end. */
  private static final String TEN = "2026-11-17T09:00:00Z";

  private static final String ELEVEN = "2026-11-17T10:00:00Z";

  /** Tue 17 Nov 2026, 09:00 in Zurich: one hour before the start, when the window opens. */
  private static final String NINE = "2026-11-17T08:00:00Z";

  private static final String TEAMS_LINK = "https://example.com/meet/expert-2";

  @Autowired MockMvcTester mockMvc;

  @Autowired TestClock clock;

  /** Now, Expert 2 is available on 17 Nov, 09:00–12:00. */
  @BeforeEach
  void setNowAndAvailability() {
    clock.set(NOW);
    available(SeedData.EXPERT_2);
  }

  private void available(UUID expert) {
    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/availability")
                .with(LoggedIn.as(expert, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "dates": ["2026-11-17"], "timeRanges": [{ "start": "09:00", "end": "12:00" }] }
                    """))
        .hasStatusOk();
  }

  /** The expert saves their profile with this Teams link, or none for {@code null}. */
  private void teamsLink(UUID expert, String displayName, String link) {
    String teamsLink = link == null ? "null" : "\"" + link + "\"";
    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/profile")
                .with(LoggedIn.as(expert, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "displayName": "%s", "bio": "", "teamsLink": %s }
                    """
                        .formatted(displayName, teamsLink)))
        .hasStatusOk();
  }

  /** The customer buys Career Coaching's 3 sessions and books the expert at this start. */
  private String caseOf(UUID customer, UUID expert, String start) throws Exception {
    MvcTestResult bought =
        mockMvc
            .post()
            .uri(CUSTOMER_CASES)
            .with(LoggedIn.as(customer, Role.CUSTOMER))
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
    booked(caseId, expert, start, customer);
    return caseId;
  }

  private void booked(String caseId, UUID expert, String start, UUID customer) {
    assertThat(
            mockMvc
                .post()
                .uri(CUSTOMER_CASES + "/{id}/appointments", caseId)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "expertId": "%s", "start": "%s" }
                    """
                        .formatted(expert, start)))
        .hasStatus(HttpStatus.CREATED);
  }

  private MvcTestResult customersCase(String caseId, UUID customer) {
    return mockMvc
        .get()
        .uri(CUSTOMER_CASES + "/{id}", caseId)
        .with(LoggedIn.as(customer, Role.CUSTOMER))
        .exchange();
  }

  private MvcTestResult expertsCase(String caseId, UUID expert) {
    return mockMvc
        .get()
        .uri(EXPERT_CASES + "/{id}", caseId)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .exchange();
  }

  /**
   * Spec 06's scenarios "The customer before the window", "The customer inside the window" and
   * "After the call", and security.md's test that the link is absent outside the window: the window
   * includes 09:00 and excludes 11:00 for the appointment 10:00–11:00.
   */
  @Test
  void theCustomerGetsTheLinkOnlyInsideTheWindow() throws Exception {
    teamsLink(SeedData.EXPERT_2, "Expert 2", TEAMS_LINK);
    String caseId = caseOf(SeedData.CUSTOMER_1, SeedData.EXPERT_2, TEN);

    // 08:59 in Zurich
    clock.set(Instant.parse(NINE).minusSeconds(60));
    MvcTestResult before = customersCase(caseId, SeedData.CUSTOMER_1);
    assertThat(before)
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "upcomingAppointments": [{
                "start": "%s", "status": "BOOKED",
                "linkAvailableFrom": "%s", "linkWindowOpen": false, "teamsLink": null
              }]
            }
            """
                .formatted(TEN, NINE));
    assertThat(before).body().asString().doesNotContain(TEAMS_LINK);

    clock.set(Instant.parse(NINE));
    assertThat(customersCase(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "upcomingAppointments": [{
                "status": "BOOKED", "linkAvailableFrom": "%s", "linkWindowOpen": true, "teamsLink": "%s"
              }]
            }
            """
                .formatted(NINE, TEAMS_LINK));

    // 10:30 in Zurich: it runs (decision 2)
    clock.set(Instant.parse("2026-11-17T09:30:00Z"));
    assertThat(customersCase(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "upcomingAppointments": [{
                "status": "CONSUMED", "linkAvailableFrom": "%s", "linkWindowOpen": true, "teamsLink": "%s"
              }]
            }
            """
                .formatted(NINE, TEAMS_LINK));

    clock.set(Instant.parse(ELEVEN));
    MvcTestResult after = customersCase(caseId, SeedData.CUSTOMER_1);
    assertThat(after)
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "upcomingAppointments": [], "pastAppointments": [{ "start": "%s" }] }
            """
                .formatted(TEN));
    assertThat(after).body().asString().doesNotContain(TEAMS_LINK);
    // Decision 19: a past appointment has no link field at all
    assertThat(after)
        .bodyJson()
        .extractingPath("$.pastAppointments[0]")
        .asMap()
        .containsOnlyKeys("id", "start", "end", "status");
  }

  /** My purchases has no link, also inside the window: only the case detail's appointment has. */
  @Test
  void myPurchasesHasNoLinkInsideTheWindow() throws Exception {
    teamsLink(SeedData.EXPERT_2, "Expert 2", TEAMS_LINK);
    caseOf(SeedData.CUSTOMER_1, SeedData.EXPERT_2, TEN);
    clock.set(Instant.parse(NINE));

    MvcTestResult myPurchases =
        mockMvc
            .get()
            .uri(CUSTOMER_CASES)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .exchange();

    assertThat(myPurchases).hasStatusOk();
    assertThat(myPurchases).body().asString().doesNotContain(TEAMS_LINK);
  }

  /**
   * Spec 06's scenario "The expert always sees the link": the expert's own link on their case,
   * before, during and after the window, with the appointment upcoming until it ends.
   */
  @Test
  void theExpertHasTheirOwnLinkAtAnyTime() throws Exception {
    teamsLink(SeedData.EXPERT_2, "Expert 2", TEAMS_LINK);
    String caseId = caseOf(SeedData.CUSTOMER_1, SeedData.EXPERT_2, TEN);

    for (Instant now :
        List.of(
            NOW,
            Instant.parse(NINE).minusSeconds(60),
            Instant.parse("2026-11-17T09:30:00Z"),
            Instant.parse(ELEVEN))) {
      clock.set(now);
      assertThat(expertsCase(caseId, SeedData.EXPERT_2))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.teamsLink")
          .isEqualTo(TEAMS_LINK);
    }
    assertThat(expertsCase(caseId, SeedData.EXPERT_2))
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "upcomingAppointments": [], "pastAppointments": [{ "start": "%s" }] }
            """
                .formatted(TEN));
  }

  /**
   * Spec 06's scenario "No link yet" (decision 10): inside the window, the customer's appointment
   * has it open without a link, and the expert's case has none.
   */
  @Test
  void withoutALinkTheWindowOpensWithoutOne() throws Exception {
    String caseId = caseOf(SeedData.CUSTOMER_1, SeedData.EXPERT_2, TEN);

    clock.set(Instant.parse(NINE).minusSeconds(60));
    assertThat(customersCase(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "upcomingAppointments": [{
                "linkAvailableFrom": "%s", "linkWindowOpen": false, "teamsLink": null
              }]
            }
            """
                .formatted(NINE));

    clock.set(Instant.parse(NINE));
    assertThat(customersCase(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "upcomingAppointments": [{
                "linkAvailableFrom": "%s", "linkWindowOpen": true, "teamsLink": null
              }]
            }
            """
                .formatted(NINE));
    assertThat(expertsCase(caseId, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.teamsLink")
        .isNull();
  }

  /**
   * Spec 06's scenario "The link changes" (D-19): the link is read when the case is shown, so each
   * of Expert 2's upcoming appointments has the new one, on both sides. At 10:00 in Zurich the
   * appointments at 10:00 and at 11:00 are both inside their windows.
   */
  @Test
  void aChangedLinkReachesEveryUpcomingAppointment() throws Exception {
    teamsLink(SeedData.EXPERT_2, "Expert 2", TEAMS_LINK);
    String caseId = caseOf(SeedData.CUSTOMER_1, SeedData.EXPERT_2, TEN);
    booked(caseId, SeedData.EXPERT_2, ELEVEN, SeedData.CUSTOMER_1);
    String newLink = "https://example.com/meet/expert-2-new";

    teamsLink(SeedData.EXPERT_2, "Expert 2", newLink);
    clock.set(Instant.parse(TEN));

    MvcTestResult customers = customersCase(caseId, SeedData.CUSTOMER_1);
    assertThat(customers)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.upcomingAppointments[*].teamsLink")
        .asList()
        .containsExactly(newLink, newLink);
    assertThat(customers).body().asString().doesNotContain(TEAMS_LINK + "\"");
    MvcTestResult experts = expertsCase(caseId, SeedData.EXPERT_2);
    assertThat(experts).hasStatusOk().bodyJson().extractingPath("$.teamsLink").isEqualTo(newLink);
    assertThat(experts)
        .bodyJson()
        .extractingPath("$.upcomingAppointments[*].start")
        .asList()
        .containsExactly(TEN, ELEVEN);
  }

  /**
   * Spec 06's scenario "An expert who booked themself" and the plan's ownership: on the customer's
   * route, Expert 1 is the customer and gets their own link only inside the window; on the expert's
   * route, always.
   */
  @Test
  void anExpertWhoBookedThemselfGetsTheLinkOnTheCustomersRouteOnlyInsideTheWindow()
      throws Exception {
    String expert1Link = "https://example.com/meet/expert-1";
    available(SeedData.EXPERT_1);
    teamsLink(SeedData.EXPERT_1, "Expert 1", expert1Link);
    String caseId = caseOf(SeedData.EXPERT_1, SeedData.EXPERT_1, TEN);

    for (Instant outside : List.of(Instant.parse(NINE).minusSeconds(60), Instant.parse(ELEVEN))) {
      clock.set(outside);
      MvcTestResult customers = customersCase(caseId, SeedData.EXPERT_1);
      assertThat(customers).hasStatusOk();
      assertThat(customers).body().asString().doesNotContain(expert1Link);
      assertThat(expertsCase(caseId, SeedData.EXPERT_1))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.teamsLink")
          .isEqualTo(expert1Link);
    }

    clock.set(Instant.parse(NINE));
    assertThat(customersCase(caseId, SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.upcomingAppointments[0].teamsLink")
        .isEqualTo(expert1Link);
  }
}
