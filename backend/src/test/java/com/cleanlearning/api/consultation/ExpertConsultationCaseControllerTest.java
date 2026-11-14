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

  private MvcTestResult get(Object caseId, UUID expert) {
    return mockMvc
        .get()
        .uri(PATH + "/{id}", caseId)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .exchange();
  }

  private void cancelled(String caseId, String appointmentId, boolean creditRefunded) {
    assertThat(
            mockMvc
                .post()
                .uri(
                    CUSTOMER_CASES + "/{id}/appointments/{appointmentId}/cancel",
                    caseId,
                    appointmentId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"creditRefunded\": %s }".formatted(creditRefunded)))
        .hasStatusOk();
  }

  private void caseCancelled(String caseId, boolean refundEligible) {
    assertThat(
            mockMvc
                .post()
                .uri(CUSTOMER_CASES + "/{id}/cancel", caseId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refundEligible\": %s }".formatted(refundEligible)))
        .hasStatusOk();
  }

  /**
   * UC-11 step 4, spec 06's scenario "The detail": a consumed appointment, one cancelled late and
   * the upcoming one on 17 Nov 2026. The terms have no price paid (decision 4), and the cancelled
   * appointment is listed with its credit forfeited (decision 14).
   */
  @Test
  void showsTheCaseToItsAssignedExpert() throws Exception {
    available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-11", "2026-11-12", "2026-11-17");
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    // Wed 11 and Thu 12 Nov 2026, 10:00 in Zurich
    String consumed =
        booked(caseId, SeedData.EXPERT_2, "2026-11-11T09:00:00Z", SeedData.CUSTOMER_1);
    String late = booked(caseId, SeedData.EXPERT_2, "2026-11-12T09:00:00Z", SeedData.CUSTOMER_1);
    String upcoming = booked(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);
    // Thu 12 Nov 2026, 09:00 in Zurich: the first has ended, the second is inside its deadline
    clock.set(Instant.parse("2026-11-12T08:00:00Z"));
    cancelled(caseId, late, false);

    MvcTestResult shown = get(caseId, SeedData.EXPERT_2);

    assertThat(shown).hasStatusOk();
    assertThat(shown)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "id": "%s",
              "customerName": "Customer 1",
              "customerEmail": "customer1@example.com",
              "offeringName": "Career Coaching",
              "sessionCount": 3,
              "sessionDurationMinutes": 60,
              "cancellationDeadlineHours": 24,
              "purchasedAt": "2026-11-09T14:00:00Z",
              "openCredits": 0,
              "totalCredits": 3,
              "cancelledAt": null,
              "upcomingAppointments": [
                { "id": "%s", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "BOOKED" }
              ],
              "pastAppointments": [
                { "id": "%s", "start": "2026-11-11T09:00:00Z", "end": "2026-11-11T10:00:00Z", "status": "CONSUMED" }
              ],
              "cancelledAppointments": [
                { "id": "%s", "start": "2026-11-12T09:00:00Z", "end": "2026-11-12T10:00:00Z", "creditRefunded": false }
              ]
            }
            """
                .formatted(caseId, upcoming, consumed, late));
    // Spec 06, decision 4: neither the price paid nor a refund outcome is the expert's
    assertThat(shown.getResponse().getContentAsString())
        .doesNotContain("pricePaid")
        .doesNotContain("refundEligible");
  }

  /**
   * Spec 06, decisions 2 and 14: a running appointment is among the upcoming ones as consumed, and
   * an appointment cancelled in time and one cancelled late are listed newest start first.
   */
  @Test
  void listsARunningAppointmentAsUpcomingAndTheCancelledOnesNewestFirst() throws Exception {
    available(
        SeedData.EXPERT_2,
        "09:00",
        "12:00",
        "2026-11-11",
        "2026-11-12",
        "2026-11-13",
        "2026-11-17");
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    // Thu 12 and Fri 13 Nov 2026, 10:00 in Zurich
    String late = booked(caseId, SeedData.EXPERT_2, "2026-11-12T09:00:00Z", SeedData.CUSTOMER_1);
    String inTime = booked(caseId, SeedData.EXPERT_2, "2026-11-13T09:00:00Z", SeedData.CUSTOMER_1);
    String upcoming = booked(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);
    cancelled(caseId, inTime, true);
    // Wed 11 Nov 2026, 10:00 in Zurich, with the credit the cancellation returned
    String running = booked(caseId, SeedData.EXPERT_2, "2026-11-11T09:00:00Z", SeedData.CUSTOMER_1);
    // Wed 11 Nov 2026, 10:30 in Zurich: the first runs, and the one on the 12th is inside its
    // deadline
    clock.set(Instant.parse("2026-11-11T09:30:00Z"));
    cancelled(caseId, late, false);

    assertThat(get(caseId, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "openCredits": 0,
              "totalCredits": 3,
              "upcomingAppointments": [
                { "id": "%s", "start": "2026-11-11T09:00:00Z", "end": "2026-11-11T10:00:00Z", "status": "CONSUMED" },
                { "id": "%s", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "BOOKED" }
              ],
              "pastAppointments": [],
              "cancelledAppointments": [
                { "id": "%s", "start": "2026-11-13T09:00:00Z", "end": "2026-11-13T10:00:00Z", "creditRefunded": true },
                { "id": "%s", "start": "2026-11-12T09:00:00Z", "end": "2026-11-12T10:00:00Z", "creditRefunded": false }
              ]
            }
            """
                .formatted(running, upcoming, inTime, late));
    // A lenient JSON comparison ignores the order of an array's elements
    assertThat(get(caseId, SeedData.EXPERT_2))
        .bodyJson()
        .extractingPath("$.upcomingAppointments[*].id")
        .asList()
        .containsExactly(running, upcoming);
    assertThat(get(caseId, SeedData.EXPERT_2))
        .bodyJson()
        .extractingPath("$.cancelledAppointments[*].id")
        .asList()
        .containsExactly(inTime, late);
  }

  /**
   * Spec 06's scenario "A cancelled case": when it was cancelled, without the refund outcome
   * (decision 4), and the appointments cancelled with it among the cancelled ones (decision 14).
   */
  @Test
  void showsACancelledCaseWithTheAppointmentsCancelledWithIt() throws Exception {
    available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-11", "2026-11-17");
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String insideDeadline =
        booked(caseId, SeedData.EXPERT_2, "2026-11-11T09:00:00Z", SeedData.CUSTOMER_1);
    String beforeDeadline = booked(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);
    // Tue 10 Nov 2026, 12:00 in Zurich: the appointment on the 11th is inside its deadline
    clock.set(Instant.parse("2026-11-10T11:00:00Z"));
    caseCancelled(caseId, false);

    MvcTestResult shown = get(caseId, SeedData.EXPERT_2);

    assertThat(shown)
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "id": "%s",
              "openCredits": 2,
              "totalCredits": 3,
              "cancelledAt": "2026-11-10T11:00:00Z",
              "upcomingAppointments": [],
              "pastAppointments": [],
              "cancelledAppointments": [
                { "id": "%s", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "creditRefunded": true },
                { "id": "%s", "start": "2026-11-11T09:00:00Z", "end": "2026-11-11T10:00:00Z", "creditRefunded": false }
              ]
            }
            """
                .formatted(caseId, beforeDeadline, insideDeadline));
    assertThat(shown)
        .bodyJson()
        .extractingPath("$.cancelledAppointments[*].id")
        .asList()
        .containsExactly(beforeDeadline, insideDeadline);
    assertThat(shown.getResponse().getContentAsString())
        .doesNotContain("pricePaid")
        .doesNotContain("refundEligible");
  }

  /**
   * Spec 06's scenario "Another expert's case", with both seeded experts (security.md): exactly
   * like an unknown id, so nobody can probe which ids exist. A case's customer who is an expert
   * doesn't see it through the expert's route either, and a case without an expert is no expert's.
   */
  @Test
  void anotherExpertsCaseIsNotFound() throws Exception {
    available(SeedData.EXPERT_1, "09:00", "12:00", "2026-11-17");
    available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-17");
    String ofCustomer1 = boughtCareerCoaching(SeedData.CUSTOMER_1);
    booked(ofCustomer1, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);
    String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
    booked(ofCustomer2, SeedData.EXPERT_1, TEN, SeedData.CUSTOMER_2);
    String ofExpert1 = boughtCareerCoaching(SeedData.EXPERT_1);
    booked(ofExpert1, SeedData.EXPERT_2, ELEVEN, SeedData.EXPERT_1);
    String withoutExpert = boughtCareerCoaching(SeedData.CUSTOMER_2);

    assertThat(get(ofCustomer1, SeedData.EXPERT_1))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Not Found",
              "status": 404,
              "detail": "Case %s not found.",
              "instance": "/api/expert/consultation-cases/%s"
            }
            """
                .formatted(ofCustomer1, ofCustomer1));
    assertThat(get(ofCustomer2, SeedData.EXPERT_2)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(get(ofExpert1, SeedData.EXPERT_1)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(get(withoutExpert, SeedData.EXPERT_1)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(get(withoutExpert, SeedData.EXPERT_2)).hasStatus(HttpStatus.NOT_FOUND);

    assertThat(get(ofCustomer1, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.customerName")
        .isEqualTo("Customer 1");
    assertThat(get(ofCustomer2, SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.customerName")
        .isEqualTo("Customer 2");
    assertThat(get(ofExpert1, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.customerName")
        .isEqualTo("Expert 1");
  }

  @Test
  void answersUnknownAndMalformedIdsWithNotFoundAndBadRequest() {
    assertThat(get("00000000-0000-0000-0000-000000000000", SeedData.EXPERT_2))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Case 00000000-0000-0000-0000-000000000000 not found.");
    assertThat(get("not-a-uuid", SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo(400);
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
