package com.cleanlearning.api.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
 * UC-06 and UC-05's case detail: a customer books an appointment on their case, and sees it with the expert and the
 * credits. Tests save availability, buy and book through the API, and change only test offerings, never Career
 * Coaching; {@link SeedData#DELETE_TEST_DATA} removes it all afterwards. "Now" is Mon 9 Nov 2026, 15:00 in Zurich, as
 * in spec 04, unless a test moves the clock. In November, Zurich is UTC+1: 10:00 in Zurich is 09:00Z.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ConsultationCaseBookingTest {

    private static final String PATH = "/api/customer/consultation-cases";

    private static final String OFFERINGS = "/api/admin/consultation-offerings";

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    /** Mon 9 Nov 2026, 15:00 in Zurich. */
    private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

    /** Tue 17 Nov 2026, 09:00, 10:00 and 11:00 in Zurich. */
    private static final String NINE = "2026-11-17T08:00:00Z";
    private static final String TEN = "2026-11-17T09:00:00Z";
    private static final String ELEVEN = "2026-11-17T10:00:00Z";

    private static final String EXPERT_1_BIO = "Career coach with ten years in tech recruiting. Helps engineers find "
            + "their next role and prepare for interviews.";

    private static final String EXPERT_2_BIO = "Leadership coach and former engineering manager. Works with new team "
            + "leads on their first year in the role.";

    private static final String NO_OPEN_CREDITS = "This case has no open credits.";
    private static final String EXPERT_CHANGED = "This case already has an expert. Please choose a time with them.";
    private static final String EXPERT_NOT_ELIGIBLE =
            "This expert is no longer available for this offering. Please choose another.";
    private static final String SLOT_NOT_AVAILABLE = "This time is no longer available.";

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    TestClock clock;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void setNow() {
        clock.set(NOW);
    }

    /** The expert saves these dates, each with the one range. */
    private void available(UUID expert, String start, String end, String... dates) {
        String body = """
                { "dates": [%s], "timeRanges": [{ "start": "%s", "end": "%s" }] }
                """.formatted(Arrays.stream(dates).map(date -> "\"" + date + "\"").collect(Collectors.joining(", ")),
                start, end);
        assertThat(mockMvc.put().uri("/api/expert/availability")
                .with(LoggedIn.as(expert, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatusOk();
    }

    /** Expert 2 is available on Tue 17 Nov 2026, 09:00–12:00 in Zurich. */
    private void expert2AvailableOnTheSeventeenth() {
        available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-17");
    }

    /** The customer buys the package; returns the new case's id. */
    private String bought(UUID customer, Object offeringId, Object packageId, String price, int sessionCount)
            throws Exception {
        MvcTestResult bought = mockMvc.post().uri(PATH)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "offeringId": "%s", "packageId": "%s", "price": "%s", "sessionCount": %d }
                        """.formatted(offeringId, packageId, price, sessionCount))
                .exchange();
        assertThat(bought).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
    }

    /** The customer buys Career Coaching's 3 sessions; returns the new case's id. */
    private String boughtCareerCoaching(UUID customer) throws Exception {
        return bought(customer, SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS, "450.00", 3);
    }

    private MvcTestResult eligibleExperts(String caseId, UUID customer) {
        return mockMvc.get().uri(PATH + "/{id}/eligible-experts", caseId)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .exchange();
    }

    private MvcTestResult bookableSlots(String caseId, Object expert, UUID customer) {
        return mockMvc.get().uri(PATH + "/{id}/bookable-slots?expertId={expertId}", caseId, expert)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .exchange();
    }

    private MvcTestResult book(String caseId, Object expert, String start, UUID customer) {
        return book(caseId, """
                { "expertId": "%s", "start": "%s" }
                """.formatted(expert, start), customer);
    }

    private MvcTestResult book(String caseId, String body, UUID customer) {
        return mockMvc.post().uri(PATH + "/{id}/appointments", caseId)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult get(String caseId, UUID customer) {
        return mockMvc.get().uri(PATH + "/{id}", caseId).with(LoggedIn.as(customer, Role.CUSTOMER)).exchange();
    }

    /** The slots' starts, as the API answers them. */
    private List<String> slotStarts(String caseId, UUID expert, UUID customer) throws Exception {
        MvcTestResult slots = bookableSlots(caseId, expert, customer);
        assertThat(slots).hasStatusOk();
        return JsonPath.read(slots.getResponse().getContentAsString(), "$[*].start");
    }

    private int countAppointments() {
        return jdbc.sql("select count(*) from appointment").query(Integer.class).single();
    }

    private UUID assignedExpert(String caseId) {
        return jdbc.sql("select assigned_expert_id from consultation_case where id = ?::uuid").param(caseId)
                .query(UUID.class).optional().orElse(null);
    }

    /** A 409 with this code and detail, as the dialog reads it. */
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

    private static void assertCaseNotFound(MvcTestResult result, String caseId) {
        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Case %s not found.",
                          "instance": "%s"
                        }
                        """.formatted(caseId, result.getRequest().getRequestURI()));
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private MvcTestResult asAdmin(MockMvcTester.MockMvcRequestBuilder request) {
        return request.with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN)).with(Csrf.token()).exchange();
    }

    /** The admin's form for Agile Coaching with its two packages and these experts eligible. */
    private static String agileCoaching(String twoSessionsId, String fourSessionsId, UUID... eligibleExperts) {
        String packages = twoSessionsId == null
                ? """
                        [{ "sessionCount": 2, "price": "300.00" }, { "sessionCount": 4, "price": "560.00" }]"""
                : """
                        [{ "id": "%s", "sessionCount": 2, "price": "300.00" },
                         { "id": "%s", "sessionCount": 4, "price": "560.00" }]""".formatted(twoSessionsId,
                        fourSessionsId);
        return """
                {
                  "name": "Agile Coaching",
                  "description": "Coaching for agile teams.",
                  "sessionDurationMinutes": 45,
                  "cancellationDeadlineHours": 48,
                  "packages": %s,
                  "eligibleExpertIds": [%s]
                }
                """.formatted(packages, Arrays.stream(eligibleExperts).map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(", ")));
    }

    /**
     * Agile Coaching, created and published through the admin's API, with 2 sessions for CHF 300.00 and 4 sessions for
     * CHF 560.00, 45 minutes and 48 hours, and Expert 2 eligible. Tests that change an offering use it.
     */
    private AgileCoaching publishAgileCoaching() throws Exception {
        MvcTestResult created = asAdmin(mockMvc.post().uri(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                .content(agileCoaching(null, null, SeedData.EXPERT_2)));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String body = created.getResponse().getContentAsString();
        AgileCoaching offering = new AgileCoaching(JsonPath.read(body, "$.id"), JsonPath.read(body, "$.packages[0].id"),
                JsonPath.read(body, "$.packages[1].id"));
        assertThat(asAdmin(mockMvc.post().uri(OFFERINGS + "/{id}/publish", offering.id()))).hasStatusOk();
        return offering;
    }

    /** The admin makes exactly these experts eligible for Agile Coaching. */
    private void makeEligible(AgileCoaching offering, UUID... experts) {
        assertThat(asAdmin(mockMvc.put().uri(OFFERINGS + "/{id}", offering.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(agileCoaching(offering.twoSessions(), offering.fourSessions(), experts))))
                .hasStatusOk();
    }

    /** Customer 1 buys Agile Coaching's 2 sessions; returns the new case's id. */
    private String boughtAgileCoaching(AgileCoaching offering) throws Exception {
        return bought(SeedData.CUSTOMER_1, offering.id(), offering.twoSessions(), "300.00", 2);
    }

    @Test
    void booksTheFirstAppointmentAndTheCaseShowsItWithTheExpertAndTheCredits() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertThat(eligibleExperts(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isStrictlyEqualTo("""
                [
                  { "id": "%s", "displayName": "Expert 1", "bio": "%s" },
                  { "id": "%s", "displayName": "Expert 2", "bio": "%s" }
                ]
                """.formatted(SeedData.EXPERT_1, EXPERT_1_BIO, SeedData.EXPERT_2, EXPERT_2_BIO));

        // 09:00–10:00 to 11:00–12:00 in Zurich; free cancellation until 24 hours before each.
        assertThat(bookableSlots(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          { "start": "2026-11-17T08:00:00Z", "end": "2026-11-17T09:00:00Z",
                            "freeCancellationUntil": "2026-11-16T08:00:00Z" },
                          { "start": "2026-11-17T08:30:00Z", "end": "2026-11-17T09:30:00Z",
                            "freeCancellationUntil": "2026-11-16T08:30:00Z" },
                          { "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z",
                            "freeCancellationUntil": "2026-11-16T09:00:00Z" },
                          { "start": "2026-11-17T09:30:00Z", "end": "2026-11-17T10:30:00Z",
                            "freeCancellationUntil": "2026-11-16T09:30:00Z" },
                          { "start": "2026-11-17T10:00:00Z", "end": "2026-11-17T11:00:00Z",
                            "freeCancellationUntil": "2026-11-16T10:00:00Z" }
                        ]
                        """);

        MvcTestResult booked = book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);

        assertThat(booked).hasStatus(HttpStatus.CREATED).doesNotContainHeader(HttpHeaders.LOCATION);
        String appointmentId = JsonPath.read(booked.getResponse().getContentAsString(), "$.id");
        String appointment = """
                { "id": "%s", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "BOOKED" }
                """.formatted(appointmentId);
        assertThat(booked).bodyJson().isStrictlyEqualTo(appointment);
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "id": "%s",
                  "expert": { "id": "%s", "displayName": "Expert 2", "bio": "%s" },
                  "openCredits": 2,
                  "totalCredits": 3,
                  "upcomingAppointments": [%s],
                  "pastAppointments": [],
                  "canBook": true
                }
                """.formatted(caseId, SeedData.EXPERT_2, EXPERT_2_BIO, appointment));
        assertThat(assignedExpert(caseId)).isEqualTo(SeedData.EXPERT_2);
    }

    @Test
    void aCaseWithoutAppointmentsHasNoExpertAndCanBook() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                {
                  "expert": null,
                  "openCredits": 3,
                  "totalCredits": 3,
                  "upcomingAppointments": [],
                  "pastAppointments": [],
                  "canBook": true
                }
                """);
    }

    @Test
    void theBookedTimeAndTheTimesOverlappingItAreGoneTouchingOnesStay() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        assertThat(book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);

        // 09:30–10:30, 10:00–11:00 and 10:30–11:30 overlap 10:00–11:00; 09:00–10:00 and 11:00–12:00 touch it.
        assertThat(slotStarts(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).containsExactly(NINE, ELEVEN);
        // Another customer's case sees the same times, and nothing of Customer 1's appointment.
        String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
        assertThat(bookableSlots(ofCustomer2, SeedData.EXPERT_2, SeedData.CUSTOMER_2)).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          { "start": "2026-11-17T08:00:00Z", "end": "2026-11-17T09:00:00Z",
                            "freeCancellationUntil": "2026-11-16T08:00:00Z" },
                          { "start": "2026-11-17T10:00:00Z", "end": "2026-11-17T11:00:00Z",
                            "freeCancellationUntil": "2026-11-16T10:00:00Z" }
                        ]
                        """);
    }

    @Test
    void aLaterBookingIsWithTheCasesExpertAndUpcomingAppointmentsComeSoonestFirst() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        assertThat(book(caseId, SeedData.EXPERT_2, ELEVEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);

        assertThat(book(caseId, SeedData.EXPERT_2, NINE, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson()
                .extractingPath("$.upcomingAppointments[*].start").asList().containsExactly(NINE, ELEVEN);
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "openCredits": 1, "canBook": true }
                """);
    }

    @Test
    void withEveryCreditBookedTheCaseCantBookAnyMore() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        for (String start : List.of(NINE, TEN, ELEVEN)) {
            assertThat(book(caseId, SeedData.EXPERT_2, start, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);
        }
        available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-17", "2026-11-18");

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "openCredits": 0, "totalCredits": 3, "canBook": false }
                """);
        // whichever expert the booking names: credits come first
        assertConflict(book(caseId, SeedData.EXPERT_2, "2026-11-18T09:00:00Z", SeedData.CUSTOMER_1),
                "NO_OPEN_CREDITS", NO_OPEN_CREDITS);
        assertConflict(book(caseId, SeedData.EXPERT_1, "2026-11-18T09:00:00Z", SeedData.CUSTOMER_1),
                "NO_OPEN_CREDITS", NO_OPEN_CREDITS);
        assertThat(countAppointments()).isEqualTo(3);
    }

    @Test
    void aTimeBookedMeanwhileByAnotherCustomerIsNotAvailable() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String ofCustomer1 = boughtCareerCoaching(SeedData.CUSTOMER_1);
        String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
        assertThat(book(ofCustomer2, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_2)).hasStatus(HttpStatus.CREATED);

        assertConflict(book(ofCustomer1, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1), "SLOT_NOT_AVAILABLE",
                SLOT_NOT_AVAILABLE);
        // an overlapping one, too
        assertConflict(book(ofCustomer1, SeedData.EXPERT_2, "2026-11-17T09:30:00Z", SeedData.CUSTOMER_1),
                "SLOT_NOT_AVAILABLE", SLOT_NOT_AVAILABLE);

        assertThat(countAppointments()).isOne();
        assertThat(assignedExpert(ofCustomer1)).isNull();
    }

    @Test
    void aStartThatIsNotABookableSlotIsNotAvailable() throws Exception {
        available(SeedData.EXPERT_2, "09:00", "12:00", "2026-11-10", "2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        // off the grid, too late to fit, outside the availability, within 24 hours (Tue 10 Nov, 10:00)
        for (String start : List.of("2026-11-17T09:15:00Z", "2026-11-17T10:30:00Z", "2026-11-18T09:00:00Z",
                "2026-11-10T09:00:00Z")) {
            assertConflict(book(caseId, SeedData.EXPERT_2, start, SeedData.CUSTOMER_1), "SLOT_NOT_AVAILABLE",
                    SLOT_NOT_AVAILABLE);
        }
        assertThat(countAppointments()).isZero();
        assertThat(assignedExpert(caseId)).isNull();
    }

    @Test
    void aFirstBookingNamingAnotherExpertThanTheCasesIsRefused() throws Exception {
        expert2AvailableOnTheSeventeenth();
        available(SeedData.EXPERT_1, "09:00", "12:00", "2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        // the first booking, in one tab
        assertThat(book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);

        // a first booking in another tab, which still showed the experts
        assertConflict(book(caseId, SeedData.EXPERT_1, TEN, SeedData.CUSTOMER_1), "EXPERT_CHANGED", EXPERT_CHANGED);
        assertConflict(bookableSlots(caseId, SeedData.EXPERT_1, SeedData.CUSTOMER_1), "EXPERT_CHANGED",
                EXPERT_CHANGED);

        assertThat(countAppointments()).isOne();
        assertThat(assignedExpert(caseId)).isEqualTo(SeedData.EXPERT_2);
    }

    @Test
    void anExpertRemovedFromTheOfferingMeanwhileIsNotEligibleOnAFirstBooking() throws Exception {
        expert2AvailableOnTheSeventeenth();
        available(SeedData.EXPERT_1, "09:00", "12:00", "2026-11-17");
        AgileCoaching offering = publishAgileCoaching();
        String caseId = boughtAgileCoaching(offering);

        makeEligible(offering, SeedData.EXPERT_1);

        assertConflict(book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1), "EXPERT_NOT_ELIGIBLE",
                EXPERT_NOT_ELIGIBLE);
        assertConflict(bookableSlots(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1), "EXPERT_NOT_ELIGIBLE",
                EXPERT_NOT_ELIGIBLE);
        assertThat(countAppointments()).isZero();
        assertThat(assignedExpert(caseId)).isNull();
        // the dialog then shows the current experts
        assertThat(eligibleExperts(caseId, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$[*].displayName").asList().containsExactly("Expert 1");
        assertThat(book(caseId, SeedData.EXPERT_1, TEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void anExpertWhoNeverWasEligibleIsNotEligible() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertConflict(book(caseId, SeedData.CUSTOMER_2, TEN, SeedData.CUSTOMER_1), "EXPERT_NOT_ELIGIBLE",
                EXPERT_NOT_ELIGIBLE);
        assertConflict(book(caseId, UNKNOWN_ID, TEN, SeedData.CUSTOMER_1), "EXPERT_NOT_ELIGIBLE",
                EXPERT_NOT_ELIGIBLE);
        assertThat(countAppointments()).isZero();
    }

    @Test
    void theCasesExpertStillBooksAfterTheyAreRemovedFromTheOffering() throws Exception {
        expert2AvailableOnTheSeventeenth();
        AgileCoaching offering = publishAgileCoaching();
        String caseId = boughtAgileCoaching(offering);
        assertThat(book(caseId, SeedData.EXPERT_2, NINE, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);

        makeEligible(offering, SeedData.EXPERT_1);

        // 45 minutes: 09:00–09:45 is booked, 10:00–10:45 is free
        assertThat(slotStarts(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).contains(TEN);
        assertThat(book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T09:45:00Z", "status": "BOOKED" }
                        """);
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "expert": { "displayName": "Expert 2" }, "openCredits": 0, "canBook": false }
                """);
    }

    @Test
    void anOfferingWithoutEligibleExpertsListsNone() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        String caseId = boughtAgileCoaching(offering);

        makeEligible(offering);

        assertThat(eligibleExperts(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isStrictlyEqualTo("[]");
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "canBook": true }
                """);
    }

    @Test
    void anExpertWithoutAvailabilityOrWithEverySlotTakenHasNoSlots() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertThat(bookableSlots(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("[]");

        available(SeedData.EXPERT_2, "10:00", "11:00", "2026-11-17");
        String ofCustomer2 = boughtCareerCoaching(SeedData.CUSTOMER_2);
        assertThat(book(ofCustomer2, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_2)).hasStatus(HttpStatus.CREATED);

        assertThat(bookableSlots(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().isStrictlyEqualTo("[]");
    }

    @Test
    void anExpertWhoIsAlsoACustomerBooksThemself() throws Exception {
        available(SeedData.EXPERT_1, "09:00", "12:00", "2026-11-17");
        String caseId = boughtCareerCoaching(SeedData.EXPERT_1);

        assertThat(book(caseId, SeedData.EXPERT_1, TEN, SeedData.EXPERT_1)).hasStatus(HttpStatus.CREATED);

        assertThat(get(caseId, SeedData.EXPERT_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "expert": { "id": "%s", "displayName": "Expert 1" }, "openCredits": 2 }
                """.formatted(SeedData.EXPERT_1));
    }

    @Test
    void aCaseOfAnArchivedOfferingBooks() throws Exception {
        expert2AvailableOnTheSeventeenth();
        AgileCoaching offering = publishAgileCoaching();
        String caseId = boughtAgileCoaching(offering);
        assertThat(asAdmin(mockMvc.post().uri(OFFERINGS + "/{id}/archive", offering.id()))).hasStatusOk();

        assertThat(eligibleExperts(caseId, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$[*].displayName").asList().containsExactly("Expert 2");
        assertThat(slotStarts(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).contains(TEN);
        assertThat(book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "expert": { "displayName": "Expert 2" }, "openCredits": 1 }
                """);
    }

    @Test
    void removingAvailabilityKeepsTheAppointment() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        assertThat(book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);

        available(SeedData.EXPERT_2, "13:00", "17:00");

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson()
                .extractingPath("$.upcomingAppointments[*].start").asList().containsExactly(TEN);
        assertThat(slotStarts(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).isEmpty();
    }

    @Test
    void anInvalidBookingNamesEachField() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertThat(book(caseId, "{}", SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Invalid request content.",
                          "instance": "%s/%s/appointments",
                          "errors": [
                            { "field": "expertId", "message": "must not be null" },
                            { "field": "start", "message": "must not be null" }
                          ]
                        }
                        """.formatted(PATH, caseId));
        assertThat(book(caseId, """
                { "expertId": "%s", "start": null }
                """.formatted(SeedData.EXPERT_2), SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").isEqualTo(List.of(error("start", "must not be null")));
        // not an instant: unreadable, without errors
        assertThat(book(caseId, SeedData.EXPERT_2, "17 Nov 2026, 10:00", SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(countAppointments()).isZero();
    }

    @Test
    void aBodyThatIsNotJsonIsAnUnsupportedMediaType() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertThat(mockMvc.post().uri(PATH + "/{id}/appointments", caseId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content("""
                        { "expertId": "%s", "start": "%s" }
                        """.formatted(SeedData.EXPERT_2, TEN)))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(countAppointments()).isZero();
    }

    @Test
    void theSlotsNeedTheExpert() throws Exception {
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        assertThat(mockMvc.get().uri(PATH + "/{id}/bookable-slots", caseId)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(bookableSlots(caseId, "not-a-uuid", SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void anotherCustomersCaseIsNotFoundOnEveryRouteAndNothingIsBooked() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);

        // Exactly like an unknown id, so nobody can probe which ids exist.
        assertCaseNotFound(eligibleExperts(caseId, SeedData.CUSTOMER_2), caseId);
        assertCaseNotFound(bookableSlots(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_2), caseId);
        assertCaseNotFound(book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_2), caseId);

        assertThat(countAppointments()).isZero();
        assertThat(assignedExpert(caseId)).isNull();
        assertThat(slotStarts(caseId, SeedData.EXPERT_2, SeedData.CUSTOMER_1)).contains(TEN);
    }

    @Test
    void unknownAndMalformedCaseIdsAreNotFoundAndBadRequests() {
        assertCaseNotFound(eligibleExperts(UNKNOWN_ID, SeedData.CUSTOMER_1), UNKNOWN_ID);
        assertCaseNotFound(bookableSlots(UNKNOWN_ID, SeedData.EXPERT_2, SeedData.CUSTOMER_1), UNKNOWN_ID);
        assertCaseNotFound(book(UNKNOWN_ID, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1), UNKNOWN_ID);

        assertThat(eligibleExperts("not-a-uuid", SeedData.CUSTOMER_1)).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(bookableSlots("not-a-uuid", SeedData.EXPERT_2, SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(book("not-a-uuid", SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(countAppointments()).isZero();
    }

    /** Spec 04, decision 21: consumed from its start, upcoming until its end, a credit used either way. */
    @Test
    void anAppointmentIsConsumedFromItsStartAndPastFromItsEnd() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        MvcTestResult booked = book(caseId, SeedData.EXPERT_2, TEN, SeedData.CUSTOMER_1);
        assertThat(booked).hasStatus(HttpStatus.CREATED);
        String appointment = """
                { "id": "%s", "start": "2026-11-17T09:00:00Z", "end": "2026-11-17T10:00:00Z", "status": "CONSUMED" }
                """.formatted((String) JsonPath.read(booked.getResponse().getContentAsString(), "$.id"));

        // Tue 17 Nov 2026, 10:30 in Zurich: it runs
        clock.set(Instant.parse("2026-11-17T09:30:00Z"));
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "upcomingAppointments": [%s], "pastAppointments": [], "openCredits": 2, "canBook": true }
                """.formatted(appointment));

        // 11:00: it has ended
        clock.set(Instant.parse(ELEVEN));
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "upcomingAppointments": [], "pastAppointments": [%s], "openCredits": 2, "canBook": true }
                """.formatted(appointment));
    }

    @Test
    void pastAppointmentsComeNewestFirst() throws Exception {
        expert2AvailableOnTheSeventeenth();
        String caseId = boughtCareerCoaching(SeedData.CUSTOMER_1);
        for (String start : List.of(TEN, NINE, ELEVEN)) {
            assertThat(book(caseId, SeedData.EXPERT_2, start, SeedData.CUSTOMER_1)).hasStatus(HttpStatus.CREATED);
        }

        clock.set(Instant.parse("2026-11-18T09:00:00Z"));

        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson()
                .extractingPath("$.pastAppointments[*].start").asList().containsExactly(ELEVEN, TEN, NINE);
        assertThat(get(caseId, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                { "upcomingAppointments": [], "openCredits": 0, "canBook": false }
                """);
    }

    /** Agile Coaching's id and package ids, as the admin's API answered them. */
    private record AgileCoaching(String id, String twoSessions, String fourSessions) {
    }
}
