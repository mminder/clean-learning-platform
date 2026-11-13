package com.cleanlearning.api.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestClock;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;

/**
 * UC-08: a customer cancels a whole case, with the outcome the dialog stated, refund-eligible or not; its future
 * appointments are cancelled with it, and a cancelled case can't be booked or cancelled again. Tests save
 * availability, buy and book through the API with the clock at Mon 9 Nov 2026, 15:00 in Zurich, then move the clock and
 * cancel, as in spec 05; {@link SeedData#DELETE_TEST_DATA} removes it all afterwards. In November, Zurich is UTC+1:
 * 10:00 in Zurich is 09:00Z.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ConsultationCaseCancellationTest {

    private static final String PATH = "/api/customer/consultation-cases";

    /** Mon 9 Nov 2026, 15:00 in Zurich: when the tests buy and book. */
    private static final Instant BOOKING_TIME = Instant.parse("2026-11-09T14:00:00Z");

    /** Sun 15 Nov 2026, 12:00 in Zurich: "now" in spec 05, in time for the appointment at 10:00 on the 17th. */
    private static final Instant SUNDAY_NOON = Instant.parse("2026-11-15T11:00:00Z");

    /** Mon 16 Nov 2026, 11:00 in Zurich: late for the appointment at 10:00 on the 17th. */
    private static final Instant MONDAY_ELEVEN = Instant.parse("2026-11-16T10:00:00Z");

    /** Tue 17 Nov 2026, 09:00, 10:00 and 11:00 in Zurich. */
    private static final String NINE = "2026-11-17T08:00:00Z";
    private static final String TEN = "2026-11-17T09:00:00Z";
    private static final String ELEVEN = "2026-11-17T10:00:00Z";

    /** Mon 16 Nov 2026, 10:00 in Zurich: 24 hours before 10:00 on the 17th, the end of its free cancellation. */
    private static final String FREE_UNTIL_TEN = "2026-11-16T09:00:00Z";

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    private static final String CASE_CANCELLED = "This case was cancelled.";
    private static final String CASE_COMPLETED = "This case is completed.";
    private static final String REFUND_ELIGIBILITY_CHANGED =
            "Whether the purchase price would be refunded has changed. Please confirm again.";

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    TestClock clock;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void setBookingTime() {
        clock.set(BOOKING_TIME);
    }

    /** Expert 2 saves these dates, each 09:00–12:00 in Zurich. */
    private void expert2Available(String... dates) {
        assertThat(mockMvc.put().uri("/api/expert/availability")
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "dates": [%s], "timeRanges": [{ "start": "09:00", "end": "12:00" }] }
                        """.formatted(String.join(", ", List.of(dates).stream()
                        .map(date -> "\"" + date + "\"").toList()))))
                .hasStatusOk();
    }

    /** The customer buys Career Coaching's 3 sessions for CHF 450.00; returns the new case's id. */
    private String boughtCareerCoaching(UUID customer) throws Exception {
        MvcTestResult bought = mockMvc.post().uri(PATH)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "offeringId": "%s", "packageId": "%s", "price": "450.00", "sessionCount": 3 }
                        """.formatted(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS))
                .exchange();
        assertThat(bought).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
    }

    /** The customer books Expert 2 at {@code start} on the case; returns the new appointment's id. */
    private String booked(String caseId, String start, UUID customer) throws Exception {
        MvcTestResult booked = book(caseId, start, customer);
        assertThat(booked).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(booked.getResponse().getContentAsString(), "$.id");
    }

    private MvcTestResult book(String caseId, String start, UUID customer) {
        return book(caseId, SeedData.EXPERT_2, start, customer);
    }

    private MvcTestResult book(String caseId, UUID expert, String start, UUID customer) {
        return mockMvc.post().uri(PATH + "/{id}/appointments", caseId)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "expertId": "%s", "start": "%s" }
                        """.formatted(expert, start))
                .exchange();
    }

    private MvcTestResult cancel(String caseId, boolean refundEligible, UUID customer) {
        return cancel(caseId, """
                { "refundEligible": %s }
                """.formatted(refundEligible), customer);
    }

    private MvcTestResult cancel(String caseId, String body, UUID customer) {
        return mockMvc.post().uri(PATH + "/{id}/cancel", caseId)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult get(String caseId, UUID customer) {
        return mockMvc.get().uri(PATH + "/{id}", caseId).with(LoggedIn.as(customer, Role.CUSTOMER)).exchange();
    }

    private MvcTestResult slots(String caseId, UUID customer) {
        return slots(caseId, SeedData.EXPERT_2, customer);
    }

    private MvcTestResult slots(String caseId, UUID expert, UUID customer) {
        return mockMvc.get()
                .uri(PATH + "/{id}/bookable-slots?expertId={expertId}", caseId, expert)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .exchange();
    }

    /** The starts of Expert 2's slots for the case, as the API answers them. */
    private List<String> slotStarts(String caseId, UUID customer) throws Exception {
        MvcTestResult slots = slots(caseId, customer);
        assertThat(slots).hasStatusOk();
        return JsonPath.read(slots.getResponse().getContentAsString(), "$[*].start");
    }

    private MvcTestResult cancelAppointment(String caseId, String appointmentId, boolean creditRefunded) {
        return mockMvc.post().uri(PATH + "/{id}/appointments/{appointmentId}/cancel", caseId, appointmentId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "creditRefunded": %s }
                        """.formatted(creditRefunded))
                .exchange();
    }

    /** The case's row: its status, its cancellation time in UTC and whether it's refund-eligible. */
    private Map<String, Object> storedCase(String caseId) {
        return jdbc.sql("""
                select status, to_char(cancelled_at at time zone 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS"Z"') as cancelled_at,
                       refund_eligible
                from consultation_case where id = ?::uuid
                """).param(caseId).query().singleRow();
    }

    private static Map<String, Object> active() {
        Map<String, Object> row = new HashMap<>();
        row.put("status", "ACTIVE");
        row.put("cancelled_at", null);
        row.put("refund_eligible", null);
        return row;
    }

    private static Map<String, Object> cancelled(Instant at, boolean refundEligible) {
        return Map.of("status", "CANCELLED", "cancelled_at", at.toString(), "refund_eligible", refundEligible);
    }

    /** The appointment's row: its status and {@code credit_refunded}, {@code null} while it's booked. */
    private Map<String, Object> storedAppointment(String appointmentId) {
        return jdbc.sql("select status, credit_refunded from appointment where id = ?::uuid").param(appointmentId)
                .query().singleRow();
    }

    private static Map<String, Object> booked() {
        Map<String, Object> row = new HashMap<>();
        row.put("status", "BOOKED");
        row.put("credit_refunded", null);
        return row;
    }

    private static Map<String, Object> cancelledAppointment(boolean creditRefunded) {
        return Map.of("status", "CANCELLED", "credit_refunded", creditRefunded);
    }

    /** A 409 with this code and detail, as the dialogs read it. */
    private static void assertConflict(MvcTestResult result, String code, String detail) {
        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Conflict",
                          "status": 409,
                          "detail": "%s",
                          "instance": "%s",
                          "code": "%s"
                        }
                        """.formatted(detail, result.getRequest().getRequestURI(), code));
    }

    @Test
    void cancelsACaseRefundEligibleWithItsFutureAppointment() throws Exception {
        expert2Available("2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
        clock.set(SUNDAY_NOON);

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "cancellation": null,
                  "canCancel": true,
                  "refundIfCancelledNow": { "refundEligible": true, "reason": null, "appointment": null },
                  "canBook": true
                }
                """);

        MvcTestResult cancelled = cancel(caseId, true, SeedData.CUSTOMER_1);

        assertThat(cancelled).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "id": "%s",
                  "cancellation": { "cancelledAt": "%s", "refundEligible": true },
                  "canCancel": false,
                  "refundIfCancelledNow": null,
                  "canBook": false,
                  "upcomingAppointments": [],
                  "pastAppointments": []
                }
                """.formatted(caseId, SUNDAY_NOON));
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "openCredits": 3,
                  "cancellation": { "cancelledAt": "%s", "refundEligible": true },
                  "canCancel": false,
                  "refundIfCancelledNow": null,
                  "canBook": false,
                  "upcomingAppointments": []
                }
                """.formatted(SUNDAY_NOON));
        assertThat(storedCase(caseId)).isEqualTo(cancelled(SUNDAY_NOON, true));
        // cancelled with the case, as UC-07 would cancel it now: in time, so its credit is refunded
        assertThat(storedAppointment(appointmentId)).isEqualTo(cancelledAppointment(true));
        // the time is offered again, to another customer
        String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
        assertThat(slotStarts(ofCustomer2, SeedData.CUSTOMER_2)).contains(TEN);
        assertThat(booked(ofCustomer2, TEN, SeedData.CUSTOMER_2)).isNotBlank();
    }

    /** Spec 05, decision 6: My purchases puts a cancelled case under Past with its cancellation time, after the active. */
    @Test
    void aCancelledCaseIsPastOnMyPurchasesAfterTheActiveOne() throws Exception {
        expert2Available("2026-11-17");
        String cancelledId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        booked(cancelledId, TEN, SeedData.CUSTOMER_1);
        String activeId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        clock.set(SUNDAY_NOON);
        assertThat(cancel(cancelledId, true, SeedData.CUSTOMER_1)).hasStatusOk();

        MvcTestResult list = mockMvc.get().uri(PATH).with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER)).exchange();

        assertThat(list).hasStatusOk();
        String body = list.getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$[*].id")).containsExactly(activeId, cancelledId);
        assertThat(JsonPath.<List<String>>read(body, "$[*].group")).containsExactly("NEEDS_BOOKING", "PAST");
        assertThat(list).bodyJson().isLenientlyEqualTo("""
                [
                  { "id": "%s", "cancelledAt": null, "nextAppointment": null },
                  { "id": "%s", "cancelledAt": "%s", "nextAppointment": null }
                ]
                """.formatted(activeId, cancelledId, SUNDAY_NOON));
        // another customer's list has none of them
        MvcTestResult ofCustomer2 = mockMvc.get().uri(PATH).with(LoggedIn.as(SeedData.CUSTOMER_2, Role.CUSTOMER))
                .exchange();
        assertThat(JsonPath.<List<String>>read(ofCustomer2.getResponse().getContentAsString(), "$[*].id"))
                .doesNotContain(activeId, cancelledId);
    }

    /** Spec 05: a case without appointments, refund-eligible. */
    @Test
    void cancelsACaseWithoutAppointments() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        clock.set(SUNDAY_NOON);

        assertThat(cancel(caseId, true, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "openCredits": 3,
                  "totalCredits": 3,
                  "purchaseTerms": { "pricePaid": { "amount": "450.00", "currency": "CHF" } },
                  "cancellation": { "cancelledAt": "%s", "refundEligible": true },
                  "canCancel": false,
                  "refundIfCancelledNow": null,
                  "canBook": false,
                  "upcomingAppointments": [],
                  "pastAppointments": []
                }
                """.formatted(SUNDAY_NOON));
        assertThat(storedCase(caseId)).isEqualTo(cancelled(SUNDAY_NOON, true));
    }

    /**
     * Spec 05, "A cancelled case's detail": a consumed appointment keeps the case from being refund-eligible; after
     * the cancellation the case lists it, but not the upcoming one cancelled with it, and has nothing to book or
     * cancel.
     */
    @Test
    void aConsumedAppointmentIsNotRefundEligibleAndStaysListedAfterTheCancellation() throws Exception {
        expert2Available("2026-11-11", "2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        // Wed 11 Nov 2026, 10:00 in Zurich
        String consumed = booked(caseId, "2026-11-11T09:00:00Z", SeedData.CUSTOMER_1);
        String upcoming = booked(caseId, TEN, SeedData.CUSTOMER_1);
        clock.set(SUNDAY_NOON);

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "canCancel": true,
                  "refundIfCancelledNow": {
                    "refundEligible": false, "reason": "SESSION_TOOK_PLACE", "appointment": null
                  }
                }
                """);
        assertThat(cancel(caseId, false, SeedData.CUSTOMER_1)).hasStatusOk();

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "openCredits": 2,
                  "cancellation": { "cancelledAt": "%s", "refundEligible": false },
                  "canCancel": false,
                  "refundIfCancelledNow": null,
                  "canBook": false,
                  "upcomingAppointments": [],
                  "pastAppointments": [{ "id": "%s", "status": "CONSUMED" }]
                }
                """.formatted(SUNDAY_NOON, consumed));
        assertThat(storedCase(caseId)).isEqualTo(cancelled(SUNDAY_NOON, false));
        assertThat(storedAppointment(consumed)).isEqualTo(booked());
        assertThat(storedAppointment(upcoming)).isEqualTo(cancelledAppointment(true));
    }

    @Test
    void aCreditCancelledLateIsNotRefundEligible() throws Exception {
        expert2Available("2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
        clock.set(MONDAY_ELEVEN);
        assertThat(cancelAppointment(caseId, appointmentId, false)).hasStatusOk();

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "canCancel": true,
                  "refundIfCancelledNow": { "refundEligible": false, "reason": "CREDIT_FORFEITED", "appointment": null }
                }
                """);
        assertThat(cancel(caseId, false, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "openCredits": 2, "cancellation": { "cancelledAt": "%s", "refundEligible": false } }
                """.formatted(MONDAY_ELEVEN));
        assertThat(storedCase(caseId)).isEqualTo(cancelled(MONDAY_ELEVEN, false));
    }

    /** Spec 05, decision 7: the appointment past its deadline is cancelled with the case, and its credit forfeited. */
    @Test
    void anAppointmentPastItsDeadlineIsNotRefundEligibleAndItsCreditIsForfeited() throws Exception {
        expert2Available("2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
        clock.set(MONDAY_ELEVEN);

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "canCancel": true,
                  "refundIfCancelledNow": {
                    "refundEligible": false,
                    "reason": "DEADLINE_PASSED",
                    "appointment": { "start": "%s", "end": "%s", "freeCancellationUntil": "%s" }
                  }
                }
                """.formatted(TEN, ELEVEN, FREE_UNTIL_TEN));
        assertThat(cancel(caseId, false, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "openCredits": 2,
                  "cancellation": { "cancelledAt": "%s", "refundEligible": false },
                  "upcomingAppointments": []
                }
                """.formatted(MONDAY_ELEVEN));
        assertThat(storedCase(caseId)).isEqualTo(cancelled(MONDAY_ELEVEN, false));
        assertThat(storedAppointment(appointmentId)).isEqualTo(cancelledAppointment(false));
    }

    /** Spec 05, "Only the first reason": a consumed appointment and one cancelled late give only the first. */
    @Test
    void onlyTheFirstReasonIsGiven() throws Exception {
        expert2Available("2026-11-11", "2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        booked(caseId, "2026-11-11T09:00:00Z", SeedData.CUSTOMER_1);
        String late = booked(caseId, TEN, SeedData.CUSTOMER_1);
        clock.set(MONDAY_ELEVEN);
        assertThat(cancelAppointment(caseId, late, false)).hasStatusOk();

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "refundIfCancelledNow": {
                    "refundEligible": false, "reason": "SESSION_TOOK_PLACE", "appointment": null
                  }
                }
                """);
    }

    /**
     * Spec 05, decision 7: every appointment that hasn't started is cancelled with the case, its credit refunded before
     * its deadline and forfeited after; a running one is consumed and stays.
     */
    @Test
    void theAppointmentsThatHaveNotStartedAreCancelledEachWithItsCredit() throws Exception {
        expert2Available("2026-11-17", "2026-11-18", "2026-11-24");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String running = booked(caseId, TEN, SeedData.CUSTOMER_1);
        // Wed 18 Nov 2026, 10:00 in Zurich: free until Tue 17 Nov, 10:00
        String late = booked(caseId, "2026-11-18T09:00:00Z", SeedData.CUSTOMER_1);
        // Tue 24 Nov 2026, 10:00 in Zurich
        String inTime = booked(caseId, "2026-11-24T09:00:00Z", SeedData.CUSTOMER_1);
        // Tue 17 Nov 2026, 10:30 in Zurich
        Instant whileItRuns = Instant.parse("2026-11-17T09:30:00Z");
        clock.set(whileItRuns);

        assertThat(cancel(caseId, false, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "openCredits": 1,
                  "cancellation": { "cancelledAt": "%s", "refundEligible": false },
                  "canCancel": false,
                  "canBook": false,
                  "upcomingAppointments": [{ "id": "%s", "status": "CONSUMED", "canCancel": false }],
                  "pastAppointments": []
                }
                """.formatted(whileItRuns, running));
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson()
                .extractingPath("$.upcomingAppointments[*].id").asList().containsExactly(running);
        assertThat(storedAppointment(running)).isEqualTo(booked());
        assertThat(storedAppointment(late)).isEqualTo(cancelledAppointment(false));
        assertThat(storedAppointment(inTime)).isEqualTo(cancelledAppointment(true));
    }

    /** Spec 05, decision 4: the dialog said the price would be refunded, but the deadline passed meanwhile. */
    @Test
    void aRefundConfirmedAfterTheDeadlineIsRefusedAndNothingChanges() throws Exception {
        expert2Available("2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
        // Mon 16 Nov 2026, 10:01 in Zurich
        clock.set(Instant.parse("2026-11-16T09:01:00Z"));

        assertConflict(cancel(caseId, true, SeedData.CUSTOMER_1), "REFUND_ELIGIBILITY_CHANGED",
                REFUND_ELIGIBILITY_CHANGED);

        assertThat(storedCase(caseId)).isEqualTo(active());
        assertThat(storedAppointment(appointmentId)).isEqualTo(booked());
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "cancellation": null,
                  "canCancel": true,
                  "refundIfCancelledNow": { "refundEligible": false, "reason": "DEADLINE_PASSED" },
                  "upcomingAppointments": [{ "id": "%s" }]
                }
                """.formatted(appointmentId));
        // the dialog confirms again, without a refund
        assertThat(cancel(caseId, false, SeedData.CUSTOMER_1)).hasStatusOk();
    }

    /** Spec 05: cancelled in one tab, then in another. */
    @Test
    void aSecondCancellationIsRefusedAndNothingChanges() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        clock.set(SUNDAY_NOON);
        assertThat(cancel(caseId, true, SeedData.CUSTOMER_1)).hasStatusOk();
        clock.set(MONDAY_ELEVEN);

        assertConflict(cancel(caseId, true, SeedData.CUSTOMER_1), "CASE_CANCELLED", CASE_CANCELLED);
        assertConflict(cancel(caseId, false, SeedData.CUSTOMER_1), "CASE_CANCELLED", CASE_CANCELLED);

        assertThat(storedCase(caseId)).isEqualTo(cancelled(SUNDAY_NOON, true));
    }

    /** Spec 05, decision 13: a fully booked case is completed once its last appointment has ended, not before. */
    @Test
    void aCompletedCaseCantBeCancelledAndNothingChanges() throws Exception {
        expert2Available("2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        booked(caseId, NINE, SeedData.CUSTOMER_1);
        booked(caseId, TEN, SeedData.CUSTOMER_1);
        String last = booked(caseId, ELEVEN, SeedData.CUSTOMER_1);
        // Tue 17 Nov 2026, 11:30 in Zurich: the last session runs, so the case can still be cancelled
        clock.set(Instant.parse("2026-11-17T10:30:00Z"));
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "canCancel": true,
                  "refundIfCancelledNow": { "refundEligible": false, "reason": "SESSION_TOOK_PLACE" },
                  "canBook": false
                }
                """);
        // Tue 17 Nov 2026, 12:00 in Zurich: it has ended
        clock.set(Instant.parse("2026-11-17T11:00:00Z"));
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "cancellation": null, "canCancel": false, "refundIfCancelledNow": null, "canBook": false }
                """);

        assertConflict(cancel(caseId, false, SeedData.CUSTOMER_1), "CASE_COMPLETED", CASE_COMPLETED);
        assertConflict(cancel(caseId, true, SeedData.CUSTOMER_1), "CASE_COMPLETED", CASE_COMPLETED);

        assertThat(storedCase(caseId)).isEqualTo(active());
        assertThat(storedAppointment(last)).isEqualTo(booked());
    }

    /** UC-06's error flow "The case was cancelled": the times and the booking of a tab left open on the case. */
    @Test
    void aCancelledCaseCantBeBooked() throws Exception {
        expert2Available("2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String withExpert = boughtCareerCoaching(SeedData.CUSTOMER_1);
        booked(withExpert, NINE, SeedData.CUSTOMER_1);
        clock.set(SUNDAY_NOON);
        assertThat(cancel(caseId, true, SeedData.CUSTOMER_1)).hasStatusOk();
        assertThat(cancel(withExpert, true, SeedData.CUSTOMER_1)).hasStatusOk();

        // without an expert, the first booking's expert would be eligible; with one, it's the case's
        for (String cancelledCase : List.of(caseId, withExpert)) {
            assertConflict(slots(cancelledCase, SeedData.CUSTOMER_1), "CASE_CANCELLED", CASE_CANCELLED);
            assertConflict(book(cancelledCase, ELEVEN, SeedData.CUSTOMER_1), "CASE_CANCELLED", CASE_CANCELLED);
        }
        // checked before the expert: with another expert than the case's, it's still CASE_CANCELLED, not EXPERT_CHANGED
        assertConflict(slots(withExpert, SeedData.EXPERT_1, SeedData.CUSTOMER_1), "CASE_CANCELLED", CASE_CANCELLED);
        assertConflict(book(withExpert, SeedData.EXPERT_1, ELEVEN, SeedData.CUSTOMER_1), "CASE_CANCELLED",
                CASE_CANCELLED);

        assertThat(jdbc.sql("""
                select count(*) from appointment
                where consultation_case_id in (?::uuid, ?::uuid) and status = 'BOOKED'
                """).params(caseId, withExpert).query(Long.class).single()).isZero();
    }

    @Test
    void anotherCustomersCaseIsNotFoundAndNothingChanges() throws Exception {
        expert2Available("2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String appointmentId = booked(caseId, TEN, SeedData.CUSTOMER_1);
        clock.set(SUNDAY_NOON);

        // Exactly like an unknown case, so nobody can probe which ids exist.
        for (MvcTestResult notFound : List.of(cancel(caseId, true, SeedData.CUSTOMER_2),
                cancel(UNKNOWN_ID, true, SeedData.CUSTOMER_1))) {
            assertThat(notFound)
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Case %s not found.",
                              "instance": "%s"
                            }
                            """.formatted(notFound.getRequest().getRequestURI().split("/")[4],
                            notFound.getRequest().getRequestURI()));
        }

        assertThat(storedCase(caseId)).isEqualTo(active());
        assertThat(storedAppointment(appointmentId)).isEqualTo(booked());
    }

    @Test
    void theRequestNamesTheOutcome() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertThat(cancel(caseId, "{}", SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Invalid request content.",
                          "instance": "%s/%s/cancel",
                          "errors": [{ "field": "refundEligible", "message": "must not be null" }]
                        }
                        """.formatted(PATH, caseId));
        // without a body, with one that isn't a boolean, or with a malformed id: unreadable, without errors
        assertThat(cancel(caseId, "", SeedData.CUSTOMER_1)).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(cancel(caseId, """
                { "refundEligible": "maybe" }
                """, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(cancel("not-a-uuid", true, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        // JSON sent as text
        assertThat(mockMvc.post().uri(PATH + "/{id}/cancel", caseId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content("""
                        { "refundEligible": true }
                        """))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(storedCase(caseId)).isEqualTo(active());
    }
}
