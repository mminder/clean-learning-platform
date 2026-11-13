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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * UC-07: a customer cancels an upcoming appointment on their case, in time with the credit
 * refunded, or late with it forfeited, and every count of open credits subtracts the forfeited
 * ones. Tests save availability, buy and book through the API with the clock at Mon 9 Nov 2026,
 * 15:00 in Zurich, then move the clock and cancel, as in spec 05; they change only test offerings,
 * never Career Coaching, and {@link SeedData#DELETE_TEST_DATA} removes it all afterwards. In
 * November, Zurich is UTC+1: 10:00 in Zurich is 09:00Z.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ConsultationAppointmentCancellationTest {

  private static final String PATH = "/api/customer/consultation-cases";

  private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

  // Mock Interview from one-session-offering.sql: published, 1 session for CHF 150.00, 60 minutes,
  // 24 hours
  private static final String ONE_SESSION_OFFERING =
      "/com/cleanlearning/api/consultation/one-session-offering.sql";
  private static final UUID MOCK_INTERVIEW =
      UUID.fromString("00000000-0000-0000-0000-000000009007");
  private static final UUID MOCK_INTERVIEW_1_SESSION =
      UUID.fromString("00000000-0000-0000-0000-000000009071");

  /** Mon 9 Nov 2026, 15:00 in Zurich: when the tests buy and book. */
  private static final Instant BOOKING_TIME = Instant.parse("2026-11-09T14:00:00Z");

  /**
   * Sun 15 Nov 2026, 12:00 in Zurich: "now" in spec 05, in time for the appointment at 10:00 on the
   * 17th.
   */
  private static final Instant SUNDAY_NOON = Instant.parse("2026-11-15T11:00:00Z");

  /** Mon 16 Nov 2026, 11:00 in Zurich: late for the appointment at 10:00 on the 17th. */
  private static final Instant MONDAY_ELEVEN = Instant.parse("2026-11-16T10:00:00Z");

  /** Tue 17 Nov 2026, 09:00, 10:00 and 11:00 in Zurich. */
  private static final String NINE = "2026-11-17T08:00:00Z";

  private static final String TEN = "2026-11-17T09:00:00Z";
  private static final String ELEVEN = "2026-11-17T10:00:00Z";

  /**
   * Mon 16 Nov 2026, 10:00 in Zurich: 24 hours before 10:00 on the 17th, the end of its free
   * cancellation.
   */
  private static final String FREE_UNTIL_TEN = "2026-11-16T09:00:00Z";

  private static final String APPOINTMENT_CANCELLED = "This appointment was already cancelled.";
  private static final String APPOINTMENT_STARTED = "This appointment can no longer be cancelled.";
  private static final String DEADLINE_PASSED =
      "The free cancellation deadline has just passed. Please confirm again.";

  @Autowired MockMvcTester mockMvc;

  @Autowired TestClock clock;

  @Autowired JdbcClient jdbc;

  @BeforeEach
  void setBookingTime() {
    clock.set(BOOKING_TIME);
  }

  /** Expert 2 saves these dates, each 09:00–12:00 in Zurich. */
  private void expert2Available(String... dates) {
    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/availability")
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "dates": [%s], "timeRanges": [{ "start": "09:00", "end": "12:00" }] }
                    """
                        .formatted(
                            Arrays.stream(dates)
                                .map(date -> "\"" + date + "\"")
                                .collect(Collectors.joining(", ")))))
        .hasStatusOk();
  }

  /** Expert 2 is available on Tue 17 Nov 2026, 09:00–12:00 in Zurich. */
  private void expert2AvailableOnTheSeventeenth() {
    expert2Available("2026-11-17");
  }

  /** The customer buys the package; returns the new case's id. */
  private String bought(
      UUID customer, Object offeringId, Object packageId, String price, int sessionCount)
      throws Exception {
    MvcTestResult bought =
        mockMvc
            .post()
            .uri(PATH)
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

  /** Customer 1 buys Mock Interview's 1 session; returns the new case's id. */
  private String boughtMockInterview() throws Exception {
    return bought(SeedData.CUSTOMER_1, MOCK_INTERVIEW, MOCK_INTERVIEW_1_SESSION, "150.00", 1);
  }

  /** The customer books Expert 2 at {@code start} on the case; returns the new appointment's id. */
  private String booked(String caseId, String start, UUID customer) throws Exception {
    MvcTestResult booked =
        mockMvc
            .post()
            .uri(PATH + "/{id}/appointments", caseId)
            .with(LoggedIn.as(customer, Role.CUSTOMER))
            .with(Csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                { "expertId": "%s", "start": "%s" }
                """
                    .formatted(SeedData.EXPERT_2, start))
            .exchange();
    assertThat(booked).hasStatus(HttpStatus.CREATED);
    return JsonPath.read(booked.getResponse().getContentAsString(), "$.id");
  }

  private MvcTestResult cancel(
      String caseId, String appointmentId, boolean creditRefunded, UUID customer) {
    return cancel(
        caseId,
        appointmentId,
        """
        { "creditRefunded": %s }
        """
            .formatted(creditRefunded),
        customer);
  }

  private MvcTestResult cancel(String caseId, String appointmentId, String body, UUID customer) {
    return mockMvc
        .post()
        .uri(PATH + "/{id}/appointments/{appointmentId}/cancel", caseId, appointmentId)
        .with(LoggedIn.as(customer, Role.CUSTOMER))
        .with(Csrf.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private MvcTestResult get(String caseId, UUID customer) {
    return mockMvc
        .get()
        .uri(PATH + "/{id}", caseId)
        .with(LoggedIn.as(customer, Role.CUSTOMER))
        .exchange();
  }

  private MvcTestResult list(UUID customer) {
    return mockMvc.get().uri(PATH).with(LoggedIn.as(customer, Role.CUSTOMER)).exchange();
  }

  /** The starts of Expert 2's slots for the case, as the API answers them. */
  private List<String> slotStarts(String caseId, UUID customer) throws Exception {
    MvcTestResult slots =
        mockMvc
            .get()
            .uri(PATH + "/{id}/bookable-slots?expertId={expertId}", caseId, SeedData.EXPERT_2)
            .with(LoggedIn.as(customer, Role.CUSTOMER))
            .exchange();
    assertThat(slots).hasStatusOk();
    return JsonPath.read(slots.getResponse().getContentAsString(), "$[*].start");
  }

  /**
   * The appointment's row: its status and {@code credit_refunded}, {@code null} while it's booked.
   */
  private Map<String, Object> stored(String appointmentId) {
    return jdbc.sql("select status, credit_refunded from appointment where id = ?::uuid")
        .param(appointmentId)
        .query()
        .singleRow();
  }

  private static Map<String, Object> booked() {
    Map<String, Object> row = new HashMap<>();
    row.put("status", "BOOKED");
    row.put("credit_refunded", null);
    return row;
  }

  /** A 409 with this code and detail, as the dialog reads it. */
  private static void assertConflict(MvcTestResult result, String code, String detail) {
    assertThat(result)
        .hasStatus(HttpStatus.CONFLICT)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Conflict",
              "status": 409,
              "detail": "%s",
              "instance": "%s",
              "code": "%s"
            }
            """
                .formatted(detail, result.getRequest().getRequestURI(), code));
  }

  private static void assertNotFound(MvcTestResult result, String detail) {
    assertThat(result)
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Not Found",
              "status": 404,
              "detail": "%s",
              "instance": "%s"
            }
            """
                .formatted(detail, result.getRequest().getRequestURI()));
  }

  @Test
  void cancelsInTimeTheCreditComesBackAndTheTimeIsFreeAgain() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    clock.set(SUNDAY_NOON);

    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "openCredits": 2,
              "upcomingAppointments": [{
                "id": "%s", "start": "%s", "end": "%s", "status": "BOOKED",
                "canCancel": true, "freeCancellationUntil": "%s", "creditRefundedIfCancelledNow": true
              }]
            }
            """
                .formatted(appointmentId, TEN, ELEVEN, FREE_UNTIL_TEN));

    MvcTestResult cancelled = cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1);

    assertThat(cancelled)
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "id": "%s",
              "expert": { "id": "%s", "displayName": "Expert 2" },
              "openCredits": 3,
              "totalCredits": 3,
              "upcomingAppointments": [],
              "pastAppointments": [],
              "canBook": true
            }
            """
                .formatted(caseId, SeedData.EXPERT_2));
    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 3, "upcomingAppointments": [], "pastAppointments": [] }
            """);
    assertThat(stored(appointmentId))
        .containsEntry("status", "CANCELLED")
        .containsEntry("credit_refunded", true);
    // the time is offered again, to this case and to another customer's
    assertThat(slotStarts(caseId, SeedData.CUSTOMER_1)).contains(TEN);
    String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
    assertThat(slotStarts(ofCustomer2, SeedData.CUSTOMER_2)).contains(TEN);
    assertThat(booked(ofCustomer2, TEN, SeedData.CUSTOMER_2)).isNotBlank();
  }

  @Test
  void cancelsLateAndTheCreditIsForfeited() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    clock.set(MONDAY_ELEVEN);

    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "upcomingAppointments": [{
                "id": "%s", "canCancel": true, "freeCancellationUntil": "%s", "creditRefundedIfCancelledNow": false
              }]
            }
            """
                .formatted(appointmentId, FREE_UNTIL_TEN));

    assertThat(cancel(caseId, appointmentId, false, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 2, "totalCredits": 3, "upcomingAppointments": [], "canBook": true }
            """);
    assertThat(stored(appointmentId))
        .containsEntry("status", "CANCELLED")
        .containsEntry("credit_refunded", false);
    assertThat(list(SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{ "id": "%s", "group": "NEEDS_BOOKING", "nextAppointment": null, "openCredits": 2 }]
            """
                .formatted(caseId));
    // the time is free, though the credit isn't back
    assertThat(slotStarts(caseId, SeedData.CUSTOMER_1)).contains(ELEVEN);
  }

  /**
   * Spec 05, decision 4: the dialog said the credit would be returned, but the deadline passed
   * meanwhile.
   */
  @Test
  void aRefundConfirmedAfterTheDeadlineIsRefusedAndNothingChanges() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    // Mon 16 Nov 2026, 10:01 in Zurich
    clock.set(Instant.parse("2026-11-16T09:01:00Z"));

    assertConflict(
        cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1),
        "DEADLINE_PASSED",
        DEADLINE_PASSED);

    assertThat(stored(appointmentId)).isEqualTo(booked());
    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "openCredits": 2,
              "upcomingAppointments": [{ "id": "%s", "canCancel": true, "creditRefundedIfCancelledNow": false }]
            }
            """
                .formatted(appointmentId));
    // the dialog confirms again, late
    assertThat(cancel(caseId, appointmentId, false, SeedData.CUSTOMER_1)).hasStatusOk();
  }

  /** Spec 05, decision 1: free cancellation ends at the deadline itself. */
  @Test
  void atTheDeadlineTheCancellationIsLate() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    clock.set(Instant.parse(FREE_UNTIL_TEN));

    assertConflict(
        cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1),
        "DEADLINE_PASSED",
        DEADLINE_PASSED);
    assertThat(cancel(caseId, appointmentId, false, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 2 }
            """);
  }

  @Test
  void aStartedAppointmentCanNoLongerBeCancelled() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    clock.set(Instant.parse(TEN));

    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "upcomingAppointments": [{
                "id": "%s", "status": "CONSUMED", "canCancel": false, "creditRefundedIfCancelledNow": false
              }]
            }
            """
                .formatted(appointmentId));
    assertConflict(
        cancel(caseId, appointmentId, false, SeedData.CUSTOMER_1),
        "APPOINTMENT_STARTED",
        APPOINTMENT_STARTED);
    assertConflict(
        cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1),
        "APPOINTMENT_STARTED",
        APPOINTMENT_STARTED);
    assertThat(stored(appointmentId)).isEqualTo(booked());
  }

  /** Spec 05: cancelled in one tab, then in another. */
  @Test
  void aSecondCancellationIsRefusedAndNothingChanges() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    clock.set(SUNDAY_NOON);
    assertThat(cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1)).hasStatusOk();

    assertConflict(
        cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1),
        "APPOINTMENT_CANCELLED",
        APPOINTMENT_CANCELLED);
    assertConflict(
        cancel(caseId, appointmentId, false, SeedData.CUSTOMER_1),
        "APPOINTMENT_CANCELLED",
        APPOINTMENT_CANCELLED);
    // also once it would have started
    clock.set(Instant.parse(ELEVEN));
    assertConflict(
        cancel(caseId, appointmentId, false, SeedData.CUSTOMER_1),
        "APPOINTMENT_CANCELLED",
        APPOINTMENT_CANCELLED);

    assertThat(stored(appointmentId))
        .containsEntry("status", "CANCELLED")
        .containsEntry("credit_refunded", true);
    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 3 }
            """);
  }

  /** Spec 05: the credit comes back to a full case. */
  @Test
  void withEveryCreditBookedACancellationInTimeEnablesBookingAgain() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    booked(caseId, NINE, SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    booked(caseId, ELEVEN, SeedData.CUSTOMER_1);
    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 0, "canBook": false }
            """);
    clock.set(SUNDAY_NOON);

    assertThat(cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 1, "canBook": true }
            """);
    assertThat(booked(caseId, TEN, SeedData.CUSTOMER_1)).isNotBlank();
  }

  /**
   * Spec 05: one consumed appointment, one cancelled in time and one cancelled late leave 1 of 3
   * credits open, and only the consumed one is listed. The booking counts the same way: one more
   * booking, then none.
   */
  @Test
  void theCreditsCountTheForfeitedCreditsAndOnlyTheConsumedAppointmentIsListed() throws Exception {
    expert2Available("2026-11-17", "2026-11-24");
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String consumed = booked(caseId, TEN, SeedData.CUSTOMER_1);
    String inTime = booked(caseId, "2026-11-24T10:00:00Z", SeedData.CUSTOMER_1);
    String late = booked(caseId, "2026-11-24T09:00:00Z", SeedData.CUSTOMER_1);
    clock.set(SUNDAY_NOON);
    assertThat(cancel(caseId, inTime, true, SeedData.CUSTOMER_1)).hasStatusOk();
    // Mon 23 Nov 2026, 11:00 in Zurich: late for 24 Nov, 10:00; 17 Nov, 10:00 is consumed and past
    clock.set(Instant.parse("2026-11-23T10:00:00Z"));
    assertThat(cancel(caseId, late, false, SeedData.CUSTOMER_1)).hasStatusOk();

    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "openCredits": 1,
              "totalCredits": 3,
              "upcomingAppointments": [],
              "pastAppointments": [{ "id": "%s", "status": "CONSUMED" }],
              "canBook": true
            }
            """
                .formatted(consumed));
    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.pastAppointments[*].id")
        .asList()
        .containsExactly(consumed);
    assertThat(list(SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [{ "id": "%s", "group": "NEEDS_BOOKING", "openCredits": 1, "totalCredits": 3 }]
            """
                .formatted(caseId));

    // the booking counts the same way: the open credit books the time freed in time, then none is
    // left
    assertThat(booked(caseId, "2026-11-24T10:00:00Z", SeedData.CUSTOMER_1)).isNotBlank();
    assertConflict(
        mockMvc
            .post()
            .uri(PATH + "/{id}/appointments", caseId)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .with(Csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                { "expertId": "%s", "start": "2026-11-24T10:30:00Z" }
                """
                    .formatted(SeedData.EXPERT_2))
            .exchange(),
        "NO_OPEN_CREDITS",
        "This case has no open credits.");
  }

  /**
   * Spec 05, decision 8: a case whose credits were all forfeited is Past on My purchases without an
   * appointment, and sorts by its purchase time, among Past's newest first: a case bought on 18 Nov
   * comes before one whose appointment was on 17 Nov, which comes before one bought on 9 Nov.
   */
  @Test
  @Sql(ONE_SESSION_OFFERING)
  @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
  void aCaseWhoseEveryCreditWasForfeitedIsPastAndSortsByItsPurchaseTime() throws Exception {
    expert2Available("2026-11-17", "2026-11-24");
    String forfeitedEarly = boughtMockInterview();
    String forfeitedAppointment = booked(forfeitedEarly, TEN, SeedData.CUSTOMER_1);
    String consumed = boughtMockInterview();
    booked(consumed, NINE, SeedData.CUSTOMER_1);
    clock.set(MONDAY_ELEVEN);
    assertThat(cancel(forfeitedEarly, forfeitedAppointment, false, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "openCredits": 0,
              "totalCredits": 1,
              "upcomingAppointments": [],
              "pastAppointments": [],
              "canBook": false
            }
            """);
    // Wed 18 Nov 2026, 12:00 in Zurich
    clock.set(Instant.parse("2026-11-18T11:00:00Z"));
    String forfeitedLate = boughtMockInterview();
    String lateAppointment = booked(forfeitedLate, "2026-11-24T09:00:00Z", SeedData.CUSTOMER_1);
    // Mon 23 Nov 2026, 11:00 in Zurich
    clock.set(Instant.parse("2026-11-23T10:00:00Z"));
    assertThat(cancel(forfeitedLate, lateAppointment, false, SeedData.CUSTOMER_1)).hasStatusOk();

    assertThat(list(SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            [
              { "id": "%s", "group": "PAST", "nextAppointment": null, "openCredits": 0, "totalCredits": 1 },
              { "id": "%s", "group": "PAST", "nextAppointment": null, "openCredits": 0, "totalCredits": 1 },
              { "id": "%s", "group": "PAST", "nextAppointment": null, "openCredits": 0, "totalCredits": 1 }
            ]
            """
                .formatted(forfeitedLate, consumed, forfeitedEarly));
    assertThat(list(SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[*].id")
        .asList()
        .containsExactly(forfeitedLate, consumed, forfeitedEarly);
  }

  /** D-07: existing purchases keep working after their offering is archived. */
  @Test
  @Sql(ONE_SESSION_OFFERING)
  @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
  void aCaseOfAnArchivedOfferingCancels() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtMockInterview();
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    assertThat(
            mockMvc
                .post()
                .uri("/api/admin/consultation-offerings/{id}/archive", MOCK_INTERVIEW)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token()))
        .hasStatusOk();
    clock.set(SUNDAY_NOON);

    assertThat(cancel(caseId, appointmentId, true, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 1, "upcomingAppointments": [], "canBook": true }
            """);
  }

  @Test
  void anotherCustomersAppointmentIsNotFoundAndNothingChanges() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
    clock.set(SUNDAY_NOON);

    // Exactly like an unknown case, so nobody can probe which ids exist.
    assertNotFound(
        cancel(caseId, appointmentId, true, SeedData.CUSTOMER_2), "Case " + caseId + " not found.");
    // under Customer 2's own case, too
    String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
    assertNotFound(
        cancel(ofCustomer2, appointmentId, true, SeedData.CUSTOMER_2),
        "Appointment " + appointmentId + " not found.");

    assertThat(stored(appointmentId)).isEqualTo(booked());
    assertThat(get(caseId, SeedData.CUSTOMER_1))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 2, "upcomingAppointments": [{ "id": "%s" }] }
            """
                .formatted(appointmentId));
  }

  @Test
  void anAppointmentOfAnotherOfTheCustomersCasesIsNotFoundUnderThisCase() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String otherCase = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String ofOtherCase = booked(otherCase, TEN, SeedData.CUSTOMER_1);
    clock.set(SUNDAY_NOON);

    assertNotFound(
        cancel(caseId, ofOtherCase, true, SeedData.CUSTOMER_1),
        "Appointment " + ofOtherCase + " not found.");

    assertThat(stored(ofOtherCase)).isEqualTo(booked());
    assertThat(cancel(otherCase, ofOtherCase, true, SeedData.CUSTOMER_1)).hasStatusOk();
  }

  @Test
  void unknownAndMalformedIdsAreNotFoundAndBadRequests() throws Exception {
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

    assertNotFound(
        cancel(UNKNOWN_ID, UNKNOWN_ID, true, SeedData.CUSTOMER_1),
        "Case " + UNKNOWN_ID + " not found.");
    assertNotFound(
        cancel(caseId, UNKNOWN_ID, true, SeedData.CUSTOMER_1),
        "Appointment " + UNKNOWN_ID + " not found.");
    assertThat(cancel("not-a-uuid", UNKNOWN_ID, true, SeedData.CUSTOMER_1))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(cancel(caseId, "not-a-uuid", true, SeedData.CUSTOMER_1))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
  }

  @Test
  void theRequestNamesTheOutcome() throws Exception {
    expert2AvailableOnTheSeventeenth();
    String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
    String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);

    assertThat(cancel(caseId, appointmentId, "{}", SeedData.CUSTOMER_1))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Invalid request content.",
              "instance": "%s/%s/appointments/%s/cancel",
              "errors": [{ "field": "creditRefunded", "message": "must not be null" }]
            }
            """
                .formatted(PATH, caseId, appointmentId));
    // without a body, or with one that isn't a boolean: unreadable, without errors
    assertThat(cancel(caseId, appointmentId, "", SeedData.CUSTOMER_1))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(
            cancel(
                caseId,
                appointmentId,
                """
                { "creditRefunded": "maybe" }
                """,
                SeedData.CUSTOMER_1))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    // JSON sent as text
    assertThat(
            mockMvc
                .post()
                .uri(PATH + "/{id}/appointments/{appointmentId}/cancel", caseId, appointmentId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content(
                    """
                    { "creditRefunded": true }
                    """))
        .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(stored(appointmentId)).isEqualTo(booked());
  }
}
