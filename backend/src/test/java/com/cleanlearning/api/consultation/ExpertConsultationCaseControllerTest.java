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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * UC-11 steps 1 to 3: an expert's **My cases**, the cases they're assigned to, in the groups and
 * the order of **My purchases**. Tests save availability, buy and book through the API, and change
 * only test offerings, never Career Coaching; {@link SeedData#DELETE_TEST_DATA} removes it all
 * afterwards. "Now" is Mon 9 Nov 2026, 15:00 in Zurich, as in spec 06, unless a test moves the
 * clock. In November, Zurich is UTC+1: 10:00 in Zurich is 09:00Z.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ExpertConsultationCaseControllerTest {

  private static final String PATH = "/api/expert/consultation-cases";

  private static final String CUSTOMER_CASES = "/api/customer/consultation-cases";

  private static final String OFFERINGS = "/api/admin/consultation-offerings";

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Tue 17 Nov 2026, 09:00, 10:00 and 11:00 in Zurich. */
  private static final String NINE = "2026-11-17T08:00:00Z";

  private static final String TEN = "2026-11-17T09:00:00Z";
  private static final String ELEVEN = "2026-11-17T10:00:00Z";

  @Autowired MockMvcTester mockMvc;

  @Autowired TestClock clock;

  @BeforeEach
  void setNow() {
    clock.set(NOW);
  }

  private MvcTestResult list(UUID expert) {
    return mockMvc.get().uri(PATH).with(LoggedIn.as(expert, Role.EXPERT)).exchange();
  }

  /** The expert saves these dates, each with the one range. */
  private void available(UUID expert, String start, String end, String... dates) {
    String body =
        """
        { "dates": [%s], "timeRanges": [{ "start": "%s", "end": "%s" }] }
        """
            .formatted(
                Arrays.stream(dates)
                    .map(date -> "\"" + date + "\"")
                    .collect(Collectors.joining(", ")),
                start,
                end);
    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/availability")
                .with(LoggedIn.as(expert, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .hasStatusOk();
  }

  /** The customer buys the package; returns the new case's id. */
  private String bought(
      UUID customer, Object offeringId, Object packageId, String price, int sessionCount)
      throws Exception {
    MvcTestResult bought =
        mockMvc
            .post()
            .uri(CUSTOMER_CASES)
            .with(LoggedIn.as(customer, Role.CUSTOMER))
            .with(Csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                { "offeringId": "%s", "packageId": "%s", "price": "%s", "sessionCount": %d }
                """
                    .formatted(offeringId, packageId, price, sessionCount))
            .exchange();
    assertThat(bought).hasStatus(HttpStatus.CREATED);
    return JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
  }

  /** The customer buys Career Coaching's 3 sessions; returns the new case's id. */
  private String boughtCareerCoaching(UUID customer) throws Exception {
    return bought(
        customer, SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS, "450.00", 3);
  }

  /** The customer books the expert on the case; returns the new appointment's id. */
  private String booked(String caseId, UUID expert, String start, UUID customer) throws Exception {
    MvcTestResult booked =
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
                    .formatted(expert, start))
            .exchange();
    assertThat(booked).hasStatus(HttpStatus.CREATED);
    return JsonPath.read(booked.getResponse().getContentAsString(), "$.id");
  }

  @Test
  void listsTheCasesTheExpertIsAssignedTo() throws Exception {
    available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-17");
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);

    MvcTestResult listed = list(SeedData.EXPERT_2);

    assertThat(listed).hasStatusOk();
    assertThat(listed)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            [
              {
                "id": "%s",
                "customerName": "Customer 1",
                "offeringName": "Career Coaching",
                "purchasedAt": "2026-11-09T14:00:00Z",
                "openCredits": 2,
                "totalCredits": 3,
                "nextAppointment": {
                  "id": "%s", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "BOOKED"
                },
                "group": "UPCOMING",
                "cancelledAt": null
              }
            ]
            """
                .formatted(caseId, appointmentId));
    assertThat(list(SeedData.EXPERT_1)).hasStatusOk().bodyJson().isStrictlyEqualTo("[]");
  }

  /**
   * Spec 06's scenario "The groups": a case with an upcoming appointment, one with open credits and
   * nothing upcoming, a completed one and a cancelled one come as Upcoming, Needs booking and Past,
   * Past newest first: the completed case by its last appointment, the cancelled one by its
   * cancellation time (decision 18).
   */
  @Test
  void groupsTheCasesAndSortsThemAsMyPurchasesDoes() throws Exception {
    available(SeedData.EXPERT_2, "09:00", "17:00", "2026-11-17", "2026-11-24");
    String completed = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String needsBooking = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String cancelled = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String upcoming = boughtCareerCoaching(SeedData.CUSTOMER_1);
    for (String start : List.of(NINE, TEN, ELEVEN)) {
      booked(completed, SeedData.EXPERT_2, start, SeedData.CUSTOMER_1);
    }
    // 13:00 in Zurich
    booked(needsBooking, SeedData.EXPERT_2, "2026-11-17T12:00:00Z", SeedData.CUSTOMER_1);
    booked(cancelled, SeedData.EXPERT_2, "2026-11-24T10:00:00Z", SeedData.CUSTOMER_1);
    assertThat(
            mockMvc
                .post()
                .uri(CUSTOMER_CASES + "/{id}/cancel", cancelled)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refundEligible\": true }"))
        .hasStatusOk();
    String next = booked(upcoming, SeedData.EXPERT_2, "2026-11-24T09:00:00Z", SeedData.CUSTOMER_1);

    // Wed 18 Nov 2026, 10:00 in Zurich: the appointments of the 17th have ended
    clock.set(Instant.parse("2026-11-18T09:00:00Z"));

    assertThat(list(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [
              {
                "id": "%s",
                "group": "UPCOMING",
                "nextAppointment": {
                  "id": "%s", "start": "2026-11-24T09:00:00Z", "end": "2026-11-24T10:00:00Z", "status": "BOOKED"
                },
                "openCredits": 2,
                "cancelledAt": null
              },
              { "id": "%s", "group": "NEEDS_BOOKING", "nextAppointment": null, "openCredits": 2 },
              { "id": "%s", "group": "PAST", "nextAppointment": null, "openCredits": 0 },
              {
                "id": "%s", "group": "PAST", "nextAppointment": null, "cancelledAt": "2026-11-09T14:00:00Z"
              }
            ]
            """
                .formatted(upcoming, next, needsBooking, completed, cancelled));
    // A lenient JSON comparison ignores the order of an array's elements
    assertThat(list(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].id")
        .asList()
        .containsExactly(upcoming, needsBooking, completed, cancelled);
  }

  /** Spec 06, decision 2: a running appointment keeps its case under Upcoming until it ends. */
  @Test
  void aRunningAppointmentKeepsItsCaseUpcomingUntilItEnds() throws Exception {
    available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-17");
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);

    // 10:30 in Zurich: it runs
    clock.set(Instant.parse("2026-11-17T09:30:00Z"));
    assertThat(list(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{
              "id": "%s",
              "group": "UPCOMING",
              "nextAppointment": {
                "id": "%s", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "CONSUMED"
              },
              "openCredits": 2
            }]
            """
                .formatted(caseId, appointmentId));

    // 11:00: it has ended
    clock.set(Instant.parse(ELEVEN));
    assertThat(list(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{ "id": "%s", "group": "NEEDS_BOOKING", "nextAppointment": null, "openCredits": 2 }]
            """
                .formatted(caseId));
  }

  /** Spec 06's scenario "Only assigned cases", and security.md's ownership with both experts. */
  @Test
  void eachExpertListsOnlyTheCasesTheyAreAssignedTo() throws Exception {
    available(SeedData.EXPERT_1, "09:00", "12:00", "2026-11-17");
    available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-17");
    String ofCustomer1 = boughtCareerCoaching(SeedData.CUSTOMER_1);
    booked(ofCustomer1, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);
    String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
    booked(ofCustomer2, SeedData.EXPERT_1, TEN, SeedData.CUSTOMER_2);
    // without an expert: no booking yet
    boughtCareerCoaching(SeedData.CUSTOMER_2);

    assertThat(list(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{ "id": "%s", "customerName": "Customer 1" }]
            """
                .formatted(ofCustomer1));
    assertThat(list(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].id")
        .asList()
        .containsExactly(ofCustomer1);
    assertThat(list(SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{ "id": "%s", "customerName": "Customer 2" }]
            """
                .formatted(ofCustomer2));
    assertThat(list(SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].id")
        .asList()
        .containsExactly(ofCustomer2);
  }

  /** Spec 06's scenario "The expert keeps their cases": the assigned expert never changes. */
  @Test
  void aCaseStaysOnItsExpertsListAfterTheyAreRemovedFromTheOffering() throws Exception {
    available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-17");
    MvcTestResult created =
        asAdmin(
            mockMvc
                .post()
                .uri(OFFERINGS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(agileCoaching(null, SeedData.EXPERT_2)));
    assertThat(created).hasStatus(HttpStatus.CREATED);
    String offeringId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    String twoSessions =
        JsonPath.read(created.getResponse().getContentAsString(), "$.packages[0].id");
    assertThat(asAdmin(mockMvc.post().uri(OFFERINGS + "/{id}/publish", offeringId))).hasStatusOk();
    String caseId = bought(SeedData.CUSTOMER_1, offeringId, twoSessions, "300.00", 2);
    booked(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);

    assertThat(
            asAdmin(
                mockMvc
                    .put()
                    .uri(OFFERINGS + "/{id}", offeringId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(agileCoaching(twoSessions, SeedData.EXPERT_1))))
        .hasStatusOk();

    assertThat(list(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{ "id": "%s", "offeringName": "Agile Coaching", "openCredits": 1, "totalCredits": 2 }]
            """
                .formatted(caseId));
    assertThat(list(SeedData.EXPERT_1)).hasStatusOk().bodyJson().isStrictlyEqualTo("[]");
  }

  /** Spec 06's scenario "An expert who booked themself" (D-06). */
  @Test
  void anExpertWhoBookedThemselfHasTheCaseOnBothLists() throws Exception {
    available(SeedData.EXPERT_1, "09:00", "12:00", "2026-11-17");
    String caseId = boughtCareerCoaching(SeedData.EXPERT_1);
    booked(caseId, SeedData.EXPERT_1, TEN, SeedData.EXPERT_1);

    assertThat(list(SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{ "id": "%s", "customerName": "Expert 1", "group": "UPCOMING" }]
            """
                .formatted(caseId));
    assertThat(
            mockMvc
                .get()
                .uri(CUSTOMER_CASES)
                .with(LoggedIn.as(SeedData.EXPERT_1, Role.EXPERT, Role.CUSTOMER)))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].id")
        .asList()
        .containsExactly(caseId);
  }

  private MvcTestResult asAdmin(MockMvcTester.MockMvcRequestBuilder request) {
    return request.with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN)).with(Csrf.token()).exchange();
  }

  /**
   * The admin's form for Agile Coaching, 2 sessions for CHF 300.00 (new, or this package), 45
   * minutes and 48 hours, with this expert eligible. A test offering, never Career Coaching.
   */
  private static String agileCoaching(String twoSessionsId, UUID eligibleExpert) {
    String id = twoSessionsId == null ? "" : "\"id\": \"%s\", ".formatted(twoSessionsId);
    return """
    {
      "name": "Agile Coaching",
      "description": "Coaching for agile teams.",
      "sessionDurationMinutes": 45,
      "cancellationDeadlineHours": 48,
      "packages": [{ %s"sessionCount": 2, "price": "300.00" }],
      "eligibleExpertIds": ["%s"]
    }
    """
        .formatted(id, eligibleExpert);
  }
}
