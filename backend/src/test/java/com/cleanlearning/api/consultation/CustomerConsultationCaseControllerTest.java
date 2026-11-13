package com.cleanlearning.api.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestImages;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;

/**
 * UC-03 and UC-05's case detail: a customer buys a package and opens the case. Tests buy through the API and never
 * change seed rows; {@link SeedData#DELETE_TEST_DATA} removes their cases and offerings afterwards.
 */
@IntegrationTest
class CustomerConsultationCaseControllerTest {

    private static final String PATH = "/api/customer/consultation-cases";

    private static final String OFFERINGS = "/api/admin/consultation-offerings";

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    private static final String NOT_AVAILABLE = "This offering is no longer available.";

    private static final String CHANGED = "This package has changed or is no longer available. Please choose again.";

    /** The buy dialog's request for Career Coaching's 3 sessions, as the shop shows them. */
    private static final String CAREER_COACHING_3_SESSIONS = purchase(SeedData.CAREER_COACHING,
            SeedData.CAREER_COACHING_3_SESSIONS, "450.00", 3);

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    JdbcClient jdbc;

    private MvcTestResult buy(String body) {
        return mockMvc.post().uri(PATH)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MockMvcTester.MockMvcRequestBuilder get(Object id, UUID customer) {
        return mockMvc.get().uri(PATH + "/{id}", id).with(LoggedIn.as(customer, Role.CUSTOMER));
    }

    /** Buys and returns the new case's id. */
    private String bought(String body) throws Exception {
        MvcTestResult bought = buy(body);
        assertThat(bought).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
    }

    private MvcTestResult asAdmin(MockMvcTester.MockMvcRequestBuilder request) {
        return request.with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN)).with(Csrf.token()).exchange();
    }

    /** The admin's form for Agile Coaching with these packages, a JSON array, and Expert 2 eligible. */
    private static String agileCoaching(String name, int sessionDurationMinutes, int cancellationDeadlineHours,
            String packages) {
        return """
                {
                  "name": "%s",
                  "description": "Coaching for agile teams.",
                  "sessionDurationMinutes": %d,
                  "cancellationDeadlineHours": %d,
                  "packages": %s,
                  "eligibleExpertIds": ["%s"]
                }
                """.formatted(name, sessionDurationMinutes, cancellationDeadlineHours, packages, SeedData.EXPERT_2);
    }

    /**
     * Agile Coaching, created and published through the admin's API, with 2 sessions for CHF 300.00 and 4 sessions for
     * CHF 560.00, 45 minutes and 48 hours. Tests that change a bought offering use it, never Career Coaching.
     */
    private AgileCoaching publishAgileCoaching() throws Exception {
        MvcTestResult created = asAdmin(mockMvc.post().uri(OFFERINGS).contentType(MediaType.APPLICATION_JSON)
                .content(agileCoaching("Agile Coaching", 45, 48, """
                        [{ "sessionCount": 2, "price": "300.00" }, { "sessionCount": 4, "price": "560.00" }]
                        """)));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String body = created.getResponse().getContentAsString();
        AgileCoaching offering = new AgileCoaching(JsonPath.read(body, "$.id"), JsonPath.read(body, "$.packages[0].id"),
                JsonPath.read(body, "$.packages[1].id"));
        assertThat(change(offering.id(), "publish")).hasStatusOk();
        return offering;
    }

    private MvcTestResult change(String offeringId, String action) {
        return asAdmin(mockMvc.post().uri(OFFERINGS + "/{id}/" + action, offeringId));
    }

    private MvcTestResult edit(String offeringId, String body) {
        return asAdmin(mockMvc.put().uri(OFFERINGS + "/{id}", offeringId).contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private int countCases() {
        return jdbc.sql("select count(*) from consultation_case").query(Integer.class).single();
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
                        """.formatted(detail, PATH, code));
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    private static String purchase(Object offeringId, Object packageId, String price, int sessionCount) {
        return """
                { "offeringId": "%s", "packageId": "%s", "price": "%s", "sessionCount": %d }
                """.formatted(offeringId, packageId, price, sessionCount);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void buysAPackageAndItsLocationAnswersTheCase() throws Exception {
        Instant before = Instant.now();

        MvcTestResult bought = buy(purchase(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS, "450.00",
                3));

        Instant after = Instant.now();
        assertThat(bought).hasStatus(HttpStatus.CREATED);
        String body = bought.getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");
        Instant purchasedAt = Instant.parse(JsonPath.read(body, "$.purchasedAt"));
        assertThat(purchasedAt).isBetween(before, after);
        String expected = """
                {
                  "id": "%s",
                  "offeringName": "Career Coaching",
                  "offeringImageId": null,
                  "purchaseTerms": {
                    "pricePaid": { "amount": "450.00", "currency": "CHF" },
                    "sessionCount": 3,
                    "sessionDurationMinutes": 60,
                    "cancellationDeadlineHours": 24
                  },
                  "purchasedAt": "%s",
                  "openCredits": 3,
                  "totalCredits": 3,
                  "expert": null,
                  "upcomingAppointments": [],
                  "pastAppointments": [],
                  "cancellation": null,
                  "canCancel": true,
                  "refundIfCancelledNow": { "refundEligible": true, "reason": null, "appointment": null },
                  "canBook": true
                }
                """.formatted(id, JsonPath.read(body, "$.purchasedAt"));
        assertThat(bought).bodyJson().isStrictlyEqualTo(expected);
        assertThat(bought).headers().hasValue(HttpHeaders.LOCATION, PATH + "/" + id);
        assertThat(mockMvc.get().uri(bought.getResponse().getHeader(HttpHeaders.LOCATION))
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER)))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo(expected);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void buyingTwiceCreatesTwoCases() throws Exception {
        String first = bought(CAREER_COACHING_3_SESSIONS);
        String second = bought(CAREER_COACHING_3_SESSIONS);

        assertThat(second).isNotEqualTo(first);
        assertThat(countCases()).isEqualTo(2);
        assertThat(get(first, SeedData.CUSTOMER_1)).hasStatusOk();
        assertThat(get(second, SeedData.CUSTOMER_1)).hasStatusOk();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void buysTheOtherPackage() {
        assertThat(buy(purchase(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_5_SESSIONS, "700", 5)))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "purchaseTerms": {
                            "pricePaid": { "amount": "700.00", "currency": "CHF" },
                            "sessionCount": 5
                          },
                          "openCredits": 5,
                          "totalCredits": 5
                        }
                        """);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void refusesAnUnpublishedOffering() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        assertThat(change(offering.id(), "unpublish")).hasStatusOk();

        assertConflict(buy(purchase(offering.id(), offering.twoSessions(), "300.00", 2)),
                "OFFERING_NOT_AVAILABLE", NOT_AVAILABLE);
        assertThat(countCases()).isZero();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void refusesAnArchivedOffering() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        assertThat(change(offering.id(), "archive")).hasStatusOk();

        assertConflict(buy(purchase(offering.id(), offering.twoSessions(), "300.00", 2)),
                "OFFERING_NOT_AVAILABLE", NOT_AVAILABLE);
        assertThat(countCases()).isZero();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void refusesAChangedPriceOrSessionCount() {
        // As if the admin had changed the package while the dialog showed CHF 500.00, or 4 sessions.
        assertConflict(buy(purchase(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS, "500.00", 3)),
                "PACKAGE_CHANGED", CHANGED);
        assertConflict(buy(purchase(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS, "450.00", 4)),
                "PACKAGE_CHANGED", CHANGED);
        assertThat(countCases()).isZero();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void refusesARemovedPackage() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        assertThat(edit(offering.id(), agileCoaching("Agile Coaching", 45, 48, """
                [{ "id": "%s", "sessionCount": 4, "price": "560.00" }]
                """.formatted(offering.fourSessions())))).hasStatusOk();

        assertConflict(buy(purchase(offering.id(), offering.twoSessions(), "300.00", 2)), "PACKAGE_CHANGED",
                CHANGED);
        assertThat(countCases()).isZero();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void refusesAPackageOfAnotherOffering() throws Exception {
        AgileCoaching offering = publishAgileCoaching();

        assertConflict(buy(purchase(SeedData.CAREER_COACHING, offering.twoSessions(), "300.00", 2)),
                "PACKAGE_CHANGED", CHANGED);
        assertThat(countCases()).isZero();
    }

    @Test
    void answersAnUnknownOfferingWithNotFound() {
        assertThat(buy(purchase(UNKNOWN_ID, SeedData.CAREER_COACHING_3_SESSIONS, "450.00", 3)))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Offering 00000000-0000-0000-0000-000000000000 not found.",
                          "instance": "/api/customer/consultation-cases"
                        }
                        """);
        assertThat(countCases()).isZero();
    }

    @Test
    void answersInvalidValuesWithEachField() {
        // Sorted by field.
        assertThat(buy("""
                { "offeringId": "%s", "packageId": null, "price": "-1", "sessionCount": 0 }
                """.formatted(SeedData.CAREER_COACHING)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Invalid request content.",
                          "instance": "/api/customer/consultation-cases",
                          "errors": [
                            { "field": "packageId", "message": "must not be null" },
                            { "field": "price", "message": "must be an amount in CHF with at most two decimals" },
                            { "field": "sessionCount", "message": "must be greater than 0" }
                          ]
                        }
                        """);
        assertThat(buy(purchase(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_3_SESSIONS, "12.345", 3)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                        error("price", "must be an amount in CHF with at most two decimals")));
        assertThat(buy("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                        error("offeringId", "must not be null"),
                        error("packageId", "must not be null"),
                        error("price", "must not be null"),
                        error("sessionCount", "must not be null")));
        assertThat(countCases()).isZero();
    }

    @Test
    void answersABodyThatIsNotJsonWithUnsupportedMediaType() {
        assertThat(mockMvc.post().uri(PATH)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content(CAREER_COACHING_3_SESSIONS))
                .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(countCases()).isZero();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void theTermsStayWhenTheOfferingChanges() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        String id = bought(purchase(offering.id(), offering.twoSessions(), "300.00", 2));
        String terms = """
                {
                  "purchaseTerms": {
                    "pricePaid": { "amount": "300.00", "currency": "CHF" },
                    "sessionCount": 2,
                    "sessionDurationMinutes": 45,
                    "cancellationDeadlineHours": 48
                  },
                  "openCredits": 2,
                  "totalCredits": 2
                }
                """;

        // the bought package's price and session count, the session duration and the deadline
        assertThat(edit(offering.id(), agileCoaching("Agile Coaching", 90, 0, """
                [
                  { "id": "%s", "sessionCount": 3, "price": "390.00" },
                  { "id": "%s", "sessionCount": 4, "price": "560.00" }
                ]
                """.formatted(offering.twoSessions(), offering.fourSessions())))).hasStatusOk();
        assertThat(get(id, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo(terms);

        // the bought package removed: no reference from the case keeps it
        assertThat(edit(offering.id(), agileCoaching("Agile Coaching", 90, 0, """
                [{ "id": "%s", "sessionCount": 4, "price": "560.00" }]
                """.formatted(offering.fourSessions())))).hasStatusOk();
        assertThat(get(id, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo(terms);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void showsTheOfferingsNameLiveAndOpensAfterItIsArchived() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        String id = bought(purchase(offering.id(), offering.twoSessions(), "300.00", 2));

        assertThat(edit(offering.id(), agileCoaching("Lean Coaching", 45, 48, """
                [
                  { "id": "%s", "sessionCount": 2, "price": "300.00" },
                  { "id": "%s", "sessionCount": 4, "price": "560.00" }
                ]
                """.formatted(offering.twoSessions(), offering.fourSessions())))).hasStatusOk();
        assertThat(get(id, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$.offeringName").isEqualTo("Lean Coaching");

        assertThat(change(offering.id(), "archive")).hasStatusOk();
        assertThat(get(id, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$.offeringName").isEqualTo("Lean Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void showsTheOfferingsImageLive() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        String id = bought(purchase(offering.id(), offering.twoSessions(), "300.00", 2));
        assertThat(get(id, SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().extractingPath("$.offeringImageId").isNull();

        MvcTestResult uploaded = mockMvc.put().uri(OFFERINGS + "/{id}/image", offering.id())
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .multipart()
                .file(new MockMultipartFile("file", "agile.png", "image/png", TestImages.png(100)))
                .exchange();
        assertThat(uploaded).hasStatusOk();
        String imageId = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.imageId");

        assertThat(get(id, SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$.offeringImageId").isEqualTo(imageId);
        assertThat(list(SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$[0].offeringImageId").isEqualTo(imageId);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void anotherCustomersCaseIsNotFound() throws Exception {
        String id = bought(CAREER_COACHING_3_SESSIONS);

        // Exactly like an unknown id, so nobody can probe which ids exist.
        assertThat(get(id, SeedData.CUSTOMER_2))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Case %s not found.",
                          "instance": "/api/customer/consultation-cases/%s"
                        }
                        """.formatted(id, id));
        assertThat(get(id, SeedData.CUSTOMER_1)).hasStatusOk();
    }

    @Test
    void answersUnknownAndMalformedIdsWithNotFoundAndBadRequest() {
        assertThat(get(UNKNOWN_ID, SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.detail")
                .isEqualTo("Case 00000000-0000-0000-0000-000000000000 not found.");
        assertThat(get("not-a-uuid", SeedData.CUSTOMER_1))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(400);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void theCaseBelongsToTheBuyer() throws Exception {
        String id = bought(CAREER_COACHING_3_SESSIONS);

        assertThat(jdbc.sql("select customer_id from consultation_case where id = ?::uuid").param(id)
                .query(UUID.class).single()).isEqualTo(SeedData.CUSTOMER_1);
    }

    private MvcTestResult list(UUID customer) {
        return mockMvc.get().uri(PATH).with(LoggedIn.as(customer, Role.CUSTOMER)).exchange();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void listsTheCustomersCasesWithoutAppointmentsAsNeedsBookingOldestFirst() throws Exception {
        String first = bought(CAREER_COACHING_3_SESSIONS);
        String second = bought(purchase(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_5_SESSIONS, "700.00", 5));

        MvcTestResult listed = list(SeedData.CUSTOMER_1);

        assertThat(listed).hasStatusOk();
        String body = listed.getResponse().getContentAsString();
        assertThat(listed).bodyJson().isStrictlyEqualTo("""
                [
                  {
                    "id": "%s",
                    "offeringName": "Career Coaching",
                    "offeringImageId": null,
                    "purchasedAt": "%s",
                    "openCredits": 3,
                    "totalCredits": 3,
                    "expertDisplayName": null,
                    "nextAppointment": null,
                    "group": "NEEDS_BOOKING",
                    "cancelledAt": null
                  },
                  {
                    "id": "%s",
                    "offeringName": "Career Coaching",
                    "offeringImageId": null,
                    "purchasedAt": "%s",
                    "openCredits": 5,
                    "totalCredits": 5,
                    "expertDisplayName": null,
                    "nextAppointment": null,
                    "group": "NEEDS_BOOKING",
                    "cancelledAt": null
                  }
                ]
                """.formatted(first, JsonPath.read(body, "$[0].purchasedAt"), second,
                JsonPath.read(body, "$[1].purchasedAt")));
        assertThat(Instant.parse(JsonPath.read(body, "$[0].purchasedAt")))
                .isBeforeOrEqualTo(Instant.parse(JsonPath.read(body, "$[1].purchasedAt")));
    }

    /**
     * The order is the purchase time, then the id, not the insertion order: the later-bought case gets an earlier
     * purchase time (a test case, never a seed row), and two cases with the same time come by id.
     */
    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void listsByPurchaseTimeThenId() throws Exception {
        String first = bought(CAREER_COACHING_3_SESSIONS);
        String second = bought(CAREER_COACHING_3_SESSIONS);
        jdbc.sql("update consultation_case set purchased_at = purchased_at - interval '1 day' where id = ?::uuid")
                .param(second).update();

        assertThat(list(SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asList().containsExactly(second, first);

        jdbc.sql("update consultation_case set purchased_at = timestamptz '2026-11-09 14:00:00Z'").update();
        List<String> byId = Stream.of(first, second).sorted().toList();

        assertThat(list(SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asList().containsExactlyElementsOf(byId);
    }

    @Test
    void listsNothingWithoutPurchases() {
        assertThat(list(SeedData.CUSTOMER_2)).hasStatusOk().bodyJson().isStrictlyEqualTo("[]");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void eachCustomerListsOnlyTheirOwnCases() throws Exception {
        String ofCustomer1 = bought(CAREER_COACHING_3_SESSIONS);
        assertThat(mockMvc.post().uri(PATH)
                .with(LoggedIn.as(SeedData.CUSTOMER_2, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(purchase(SeedData.CAREER_COACHING, SeedData.CAREER_COACHING_5_SESSIONS, "700.00", 5)))
                .hasStatus(HttpStatus.CREATED);

        assertThat(list(SeedData.CUSTOMER_1)).hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asList().containsExactly(ofCustomer1);
        assertThat(list(SeedData.CUSTOMER_2)).hasStatusOk()
                .bodyJson().extractingPath("$[*].totalCredits").asList().containsExactly(5);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void listsACaseOfAnArchivedOfferingWithItsName() throws Exception {
        AgileCoaching offering = publishAgileCoaching();
        String id = bought(purchase(offering.id(), offering.twoSessions(), "300.00", 2));
        assertThat(change(offering.id(), "archive")).hasStatusOk();

        assertThat(list(SeedData.CUSTOMER_1)).hasStatusOk().bodyJson().isLenientlyEqualTo("""
                [{ "id": "%s", "offeringName": "Agile Coaching", "openCredits": 2, "totalCredits": 2 }]
                """.formatted(id));
    }

    /** Agile Coaching's id and package ids, as the admin's API answered them. */
    private record AgileCoaching(String id, String twoSessions, String fourSessions) {
    }
}
