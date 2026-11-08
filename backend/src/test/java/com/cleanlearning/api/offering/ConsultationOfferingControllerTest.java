package com.cleanlearning.api.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;

/**
 * UC-16, the admin's consultation offerings. All integration tests share one database, so tests never change seed
 * rows: a test that writes creates its own offering, through the API or with {@code @Sql}, and
 * {@link SeedData#DELETE_TEST_OFFERINGS} removes it afterwards. The {@code @Sql} scripts are the shop tests'.
 */
@IntegrationTest
class ConsultationOfferingControllerTest {

    private static final String PATH = "/api/admin/consultation-offerings";

    private static final String SHOP = "/com/cleanlearning/api/shop/";

    private static final String AGILE_COACHING = """
            {
              "name": "Agile Coaching",
              "description": "Coaching for agile teams.",
              "sessionDurationMinutes": 45,
              "cancellationDeadlineHours": 48,
              "packages": [],
              "eligibleExpertIds": []
            }
            """;

    private static final String LEAN_COACHING = """
            {
              "name": "Lean Coaching",
              "description": "Coaching for lean teams.",
              "sessionDurationMinutes": 90,
              "cancellationDeadlineHours": 0,
              "packages": [],
              "eligibleExpertIds": []
            }
            """;

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    JdbcClient jdbc;

    private MockMvcTester.MockMvcRequestBuilder list() {
        return mockMvc.get().uri(PATH).with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN));
    }

    private MockMvcTester.MockMvcRequestBuilder get(Object id) {
        return mockMvc.get().uri(PATH + "/{id}", id).with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN));
    }

    private MvcTestResult post(String body) {
        return mockMvc.post().uri(PATH)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult put(Object id, String body) {
        return mockMvc.put().uri(PATH + "/{id}", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    /** Creates Agile Coaching through the API and returns its id. */
    private String createAgileCoaching() throws Exception {
        MvcTestResult created = post(AGILE_COACHING);
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void listsTheSeededOfferingWithItsCounts() {
        assertThat(list())
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          {
                            "id": "00000000-0000-0000-0000-000000001001",
                            "name": "Career Coaching",
                            "status": "PUBLISHED",
                            "packageCount": 2,
                            "eligibleExpertCount": 2
                          }
                        ]
                        """);
    }

    @Test
    @Sql(SHOP + "draft-and-archived-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void listsEveryStatusSortedByName() {
        assertThat(list())
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          {
                            "id": "00000000-0000-0000-0000-000000009002",
                            "name": "Archived Coaching",
                            "status": "ARCHIVED",
                            "packageCount": 1,
                            "eligibleExpertCount": 0
                          },
                          {
                            "id": "00000000-0000-0000-0000-000000001001",
                            "name": "Career Coaching",
                            "status": "PUBLISHED",
                            "packageCount": 2,
                            "eligibleExpertCount": 2
                          },
                          {
                            "id": "00000000-0000-0000-0000-000000009001",
                            "name": "Draft Coaching",
                            "status": "DRAFT",
                            "packageCount": 1,
                            "eligibleExpertCount": 0
                          }
                        ]
                        """);
    }

    @Test
    @Sql(SHOP + "more-published-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void sortsAlphabeticallyRegardlessOfUmlauts() {
        assertThat(list())
                .hasStatusOk()
                .bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Agile Coaching", "Ärger-Coaching", "Career Coaching");
    }

    @Test
    @Sql(SHOP + "more-experts-and-packages.sql")
    @Sql(scripts = SHOP + "delete-more-experts-and-packages.sql", executionPhase = AFTER_TEST_METHOD)
    void countsPackagesAndExpertsWithoutMultiplyingThem() {
        // 3 packages and 4 experts: joining both would give 12 rows.
        assertThat(list())
                .hasStatusOk()
                .bodyJson().extractingPath("$[0].packageCount").isEqualTo(3);
        assertThat(list())
                .bodyJson().extractingPath("$[0].eligibleExpertCount").isEqualTo(4);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void createsADraftThatItsLocationAnswers() throws Exception {
        MvcTestResult created = post(AGILE_COACHING);

        assertThat(created).hasStatus(HttpStatus.CREATED);
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        String expected = """
                {
                  "id": "%s",
                  "name": "Agile Coaching",
                  "description": "Coaching for agile teams.",
                  "status": "DRAFT",
                  "sessionDurationMinutes": 45,
                  "cancellationDeadlineHours": 48,
                  "packages": [],
                  "eligibleExpertIds": [],
                  "imageId": null
                }
                """.formatted(id);
        assertThat(created).bodyJson().isStrictlyEqualTo(expected);
        assertThat(created).headers().hasValue(HttpHeaders.LOCATION, PATH + "/" + id);
        assertThat(mockMvc.get().uri(created.getResponse().getHeader(HttpHeaders.LOCATION))
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN)))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo(expected);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Agile Coaching", "Career Coaching");
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    void getsTheSeededOffering() {
        assertThat(get(SeedData.CAREER_COACHING))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "id": "00000000-0000-0000-0000-000000001001",
                          "name": "Career Coaching",
                          "status": "PUBLISHED",
                          "sessionDurationMinutes": 60,
                          "cancellationDeadlineHours": 24
                        }
                        """);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void editsAnOffering() throws Exception {
        String id = createAgileCoaching();

        MvcTestResult edited = put(id, LEAN_COACHING);

        String expected = """
                {
                  "id": "%s",
                  "name": "Lean Coaching",
                  "description": "Coaching for lean teams.",
                  "status": "DRAFT",
                  "sessionDurationMinutes": 90,
                  "cancellationDeadlineHours": 0,
                  "packages": [],
                  "eligibleExpertIds": [],
                  "imageId": null
                }
                """.formatted(id);
        assertThat(edited).hasStatusOk().bodyJson().isStrictlyEqualTo(expected);
        assertThat(get(id)).hasStatusOk().bodyJson().isStrictlyEqualTo(expected);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void anEditIgnoresAStatusInTheBody() throws Exception {
        String id = createAgileCoaching();

        MvcTestResult edited = put(id, """
                { "name": "Agile Coaching", "description": "", "sessionDurationMinutes": 45,
                  "cancellationDeadlineHours": 48, "packages": [], "eligibleExpertIds": [], "status": "PUBLISHED" }
                """);

        // the status changes only through publish, unpublish and archive
        assertThat(edited).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("DRAFT");
        assertThat(get(id)).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("DRAFT");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void savesAnEmptyDescription() {
        assertThat(post("""
                { "name": "Agile Coaching", "description": "", "sessionDurationMinutes": 45,
                  "cancellationDeadlineHours": 48, "packages": [], "eligibleExpertIds": [] }
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.description").isEqualTo("");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersInvalidValuesWithEachField() {
        // Sorted by field. A browser's Accept-Language doesn't change the messages: the UI is English (D-20).
        MvcTestResult result = mockMvc.post().uri(PATH)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .header(HttpHeaders.ACCEPT_LANGUAGE, "de-CH")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": " ", "description": "", "sessionDurationMinutes": 0,
                          "cancellationDeadlineHours": -1, "packages": [], "eligibleExpertIds": [] }
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Invalid request content.",
                          "instance": "/api/admin/consultation-offerings",
                          "errors": [
                            { "field": "cancellationDeadlineHours", "message": "must be greater than or equal to 0" },
                            { "field": "name", "message": "must not be blank" },
                            { "field": "sessionDurationMinutes", "message": "must be greater than 0" }
                          ]
                        }
                        """);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersMissingValuesWithEachField() {
        // An empty number field in the form is sent as null.
        assertThat(post("""
                { "name": null, "description": null, "sessionDurationMinutes": null,
                  "cancellationDeadlineHours": null, "packages": null, "eligibleExpertIds": null }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                        error("cancellationDeadlineHours", "must not be null"),
                        error("description", "must not be null"),
                        error("eligibleExpertIds", "must not be null"),
                        error("name", "must not be blank"),
                        error("packages", "must not be null"),
                        error("sessionDurationMinutes", "must not be null")));
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersADecimalNumberWithBadRequest() {
        // Not cut to 45 and saved: a whole-number field doesn't accept a decimal (accept-float-as-int).
        assertThat(post("""
                { "name": "Agile Coaching", "description": "", "sessionDurationMinutes": 45.5,
                  "cancellationDeadlineHours": 48, "packages": [], "eligibleExpertIds": [] }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Failed to read request",
                          "instance": "/api/admin/consultation-offerings"
                        }
                        """);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersInvalidValuesOfAnEditWithEachFieldAndKeepsTheOffering() throws Exception {
        String id = createAgileCoaching();

        assertThat(put(id, """
                { "name": "", "description": "", "sessionDurationMinutes": -5, "cancellationDeadlineHours": 1,
                  "packages": [], "eligibleExpertIds": [] }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                        error("name", "must not be blank"),
                        error("sessionDurationMinutes", "must be greater than 0")));
        assertThat(get(id)).bodyJson().extractingPath("$.name").isEqualTo("Agile Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersANameOfOnlyNoBreakSpacesWithBadRequest() {
        // Bean Validation's @NotBlank lets it through; OfferingName rejects it.
        assertThat(post("""
                { "name": "\u00A0\u202F", "description": "", "sessionDurationMinutes": 45,
                  "cancellationDeadlineHours": 48, "packages": [], "eligibleExpertIds": [] }
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "An offering name is not blank",
                          "instance": "/api/admin/consultation-offerings"
                        }
                        """);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    void answersUnknownIdsWithNotFound() {
        assertThat(get(UNKNOWN_ID))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Offering 00000000-0000-0000-0000-000000000000 not found.",
                          "instance": "/api/admin/consultation-offerings/00000000-0000-0000-0000-000000000000"
                        }
                        """);
        assertThat(put(UNKNOWN_ID, AGILE_COACHING))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(404);
    }

    @Test
    void answersMalformedIdsWithBadRequest() {
        assertThat(get("not-a-uuid"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isLenientlyEqualTo("""
                        { "status": 400, "instance": "/api/admin/consultation-offerings/not-a-uuid" }
                        """);
        assertThat(put("not-a-uuid", AGILE_COACHING))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(400);
    }

    @Test
    @Sql(SHOP + "more-published-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void editsOfAPublishedOfferingShowInTheShopAtOnce() {
        String agileCoaching = "00000000-0000-0000-0000-000000009003";

        // PUT replaces the packages, too: the request keeps the two it has, or the offering would leave the shop.
        assertThat(put(agileCoaching, LEAN_COACHING.replace("\"packages\": []", """
                "packages": [
                  { "id": "00000000-0000-0000-0000-000000009031", "sessionCount": 1, "price": "250.00" },
                  { "id": "00000000-0000-0000-0000-000000009032", "sessionCount": 4, "price": "900.00" }
                ]
                """))).hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");

        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Ärger-Coaching", "Career Coaching", "Lean Coaching");
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", agileCoaching))
                .hasStatusOk()
                .bodyJson().extractingPath("$.consultation.sessionDurationMinutes").isEqualTo(90);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void anEditChangesUpdatedAtButNotCreatedAt() throws Exception {
        String id = createAgileCoaching();
        List<Timestamps> before = timestamps(id);

        assertThat(put(id, LEAN_COACHING)).hasStatusOk();

        List<Timestamps> after = timestamps(id);
        assertThat(after).hasSize(2);
        for (int table = 0; table < before.size(); table++) {
            assertThat(after.get(table).createdAt()).isEqualTo(before.get(table).createdAt());
            assertThat(after.get(table).updatedAt()).isAfter(before.get(table).updatedAt());
        }
    }

    // Packages (task 04)

    private static final String PRICE_MESSAGE = "must be an amount in CHF with at most two decimals";

    /** The request body for an offering with these packages, a JSON array, and no eligible experts. */
    private static String withPackages(String packages) {
        return withPackagesAndExperts(packages, "[]");
    }

    private static String withPackagesAndExperts(String packages, String eligibleExpertIds) {
        return """
                {
                  "name": "Agile Coaching",
                  "description": "Coaching for agile teams.",
                  "sessionDurationMinutes": 45,
                  "cancellationDeadlineHours": 48,
                  "packages": %s,
                  "eligibleExpertIds": %s
                }
                """.formatted(packages, eligibleExpertIds);
    }

    private static String aPackage(String id, int sessionCount, String price) {
        return """
                { "id": "%s", "sessionCount": %d, "price": "%s" }""".formatted(id, sessionCount, price);
    }

    private static String aNewPackage(int sessionCount, String price) {
        return """
                { "sessionCount": %d, "price": "%s" }""".formatted(sessionCount, price);
    }

    private static String json(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void createsAnOfferingWithPackagesFewestSessionsFirst() throws Exception {
        MvcTestResult created = post(withPackages("[" + aNewPackage(4, "600") + "," + aNewPackage(1, "160.5") + "]"));

        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.packages[*].sessionCount").asArray().containsExactly(1, 4);
        assertThat(created).bodyJson().extractingPath("$.packages[*].price").asArray().containsExactly(
                Map.of("amount", "160.50", "currency", "CHF"), Map.of("amount", "600.00", "currency", "CHF"));
        String id = json(created, "$.id");
        MvcTestResult read = get(id).exchange();
        assertThat(read.getResponse().getContentAsString()).isEqualTo(created.getResponse().getContentAsString());
        assertThat(list()).bodyJson().extractingPath("$[0].packageCount").isEqualTo(2);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void anEditMatchesPackagesByIdAndWritesOnlyTheDifference() throws Exception {
        MvcTestResult created = post(withPackages(
                "[" + aNewPackage(1, "160") + "," + aNewPackage(3, "450") + "," + aNewPackage(5, "700") + "]"));
        String id = json(created, "$.id");
        String first = json(created, "$.packages[0].id");
        String second = json(created, "$.packages[1].id");
        String third = json(created, "$.packages[2].id");
        List<Timestamps> before = packageTimestamps(id);

        // changes the first, leaves out the second, keeps the third as it is and adds a new one
        MvcTestResult edited = put(id, withPackages("[" + aPackage(first, 2, "300.00") + ","
                + aPackage(third, 5, "700") + "," + aNewPackage(10, "1000") + "]"));

        assertThat(edited).hasStatusOk();
        assertThat(edited).bodyJson().extractingPath("$.packages[*].sessionCount").asArray()
                .containsExactly(2, 5, 10);
        assertThat(edited).bodyJson().extractingPath("$.packages[0].id").isEqualTo(first);
        assertThat(edited).bodyJson().extractingPath("$.packages[0].price.amount").isEqualTo("300.00");
        assertThat(edited).bodyJson().extractingPath("$.packages[1].id").isEqualTo(third);
        String added = json(edited, "$.packages[2].id");
        assertThat(added).isNotIn(first, second, third);
        assertThat(get(id)).bodyJson().extractingPath("$.packages[*].id").asArray()
                .containsExactly(first, third, added);

        // The kept packages are updated or left alone, never deleted and inserted again.
        List<Timestamps> after = packageTimestamps(id);
        assertThat(after).hasSize(3);
        assertThat(after.get(0).createdAt()).isEqualTo(before.get(0).createdAt());
        assertThat(after.get(0).updatedAt()).isAfter(before.get(0).updatedAt());
        assertThat(after.get(1).createdAt()).isEqualTo(before.get(2).createdAt());
        assertThat(after.get(1).updatedAt()).isEqualTo(before.get(2).updatedAt());
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void packagesMayShareASessionCountAndAreSortedByIdThen() throws Exception {
        MvcTestResult created = post(withPackages("[" + aNewPackage(3, "450") + "," + aNewPackage(3, "400") + "]"));

        assertThat(created).hasStatus(HttpStatus.CREATED);
        List<String> ids = JsonPath.read(created.getResponse().getContentAsString(), "$.packages[*].id");
        assertThat(ids).isSorted().hasSize(2);
        assertThat(get(json(created, "$.id"))).bodyJson().extractingPath("$.packages[*].id").asArray()
                .containsExactlyElementsOf(ids);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersInvalidPackagesWithEachFieldOfEachRow() {
        String body = withPackages("[" + aNewPackage(1, "160") + "," + aNewPackage(0, "-1") + ","
                + aNewPackage(2, "12.345") + "," + aNewPackage(3, "123456789") + "," + aNewPackage(4, "") + ","
                + aNewPackage(5, "1e2") + "," + aNewPackage(6, "12,5") + ","
                + "{ \"sessionCount\": null, \"price\": null }]");

        assertThat(post(body))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                        error("packages[1].price", PRICE_MESSAGE),
                        error("packages[1].sessionCount", "must be greater than 0"),
                        error("packages[2].price", PRICE_MESSAGE),
                        error("packages[3].price", PRICE_MESSAGE),
                        error("packages[4].price", PRICE_MESSAGE),
                        error("packages[5].price", PRICE_MESSAGE),
                        error("packages[6].price", PRICE_MESSAGE),
                        error("packages[7].price", "must not be null"),
                        error("packages[7].sessionCount", "must not be null")));
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void acceptsTheLargestAndTheSmallestPrice() {
        assertThat(post(withPackages("[" + aNewPackage(1, "99999999.99") + "," + aNewPackage(2, "0") + "]")))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.packages[*].price.amount").asArray()
                .containsExactly("99999999.99", "0.00");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersAMissingPackageEntryWithBadRequest() {
        assertThat(post(withPackages("[null]")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors[*].message").asArray().containsExactly("must not be null");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersAPackageIdOfAnotherOfferingWithBadRequestAndChangesNothing() throws Exception {
        String id = createAgileCoaching();
        String foreign = SeedData.CAREER_COACHING_3_SESSIONS.toString();

        assertThat(put(id, withPackages("[" + aPackage(foreign, 3, "1") + "]")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Package %s is not a package of this offering.",
                          "instance": "%s/%s"
                        }
                        """.formatted(foreign, PATH, id));
        assertThat(put(id, withPackages("[" + aPackage(UNKNOWN_ID, 3, "1") + "]"))).hasStatus(HttpStatus.BAD_REQUEST);
        // Career Coaching's package is untouched.
        assertThat(get(SeedData.CAREER_COACHING)).bodyJson().extractingPath("$.packages[*].price.amount").asArray()
                .containsExactly("450.00", "700.00");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersAPackageNamedTwiceWithBadRequest() throws Exception {
        String id = json(post(withPackages("[" + aNewPackage(1, "160") + "]")), "$.id");
        String pkg = json(get(id).exchange(), "$.packages[0].id");

        assertThat(put(id, withPackages("[" + aPackage(pkg, 1, "160") + "," + aPackage(pkg, 2, "300") + "]")))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(get(id)).bodyJson().extractingPath("$.packages[*].price.amount").asArray().containsExactly("160.00");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersAnyPackageIdOnPostWithBadRequest() {
        assertThat(post(withPackages("[" + aPackage(UNKNOWN_ID, 1, "160") + "]")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql(SHOP + "more-published-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void aChangedPriceOfAPublishedOfferingShowsInTheShopAtOnce() {
        String agileCoaching = "00000000-0000-0000-0000-000000009003";
        String cheapest = "00000000-0000-0000-0000-000000009031";
        String other = "00000000-0000-0000-0000-000000009032";

        assertThat(put(agileCoaching, withPackages("[" + aPackage(cheapest, 1, "199.90") + ","
                + aPackage(other, 4, "900") + "]"))).hasStatusOk();

        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[0].price").isEqualTo(Map.of("amount", "199.90", "currency", "CHF"));
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", agileCoaching))
                .bodyJson().extractingPath("$.consultation.packages[0].price.amount").isEqualTo("199.90");
    }

    @Test
    @Sql(SHOP + "more-published-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void removingEveryPackageOfAPublishedOfferingHidesItFromTheShop() {
        String agileCoaching = "00000000-0000-0000-0000-000000009003";

        assertThat(put(agileCoaching, withPackages("[]"))).hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");

        assertThat(get(agileCoaching)).bodyJson().extractingPath("$.packages").asArray().isEmpty();
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly("Ärger-Coaching", "Career Coaching");
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", agileCoaching)).hasStatus(HttpStatus.NOT_FOUND);

        // a package added again brings it back
        assertThat(put(agileCoaching, withPackages("[" + aNewPackage(2, "300") + "]"))).hasStatusOk();
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", agileCoaching)).hasStatusOk();
    }

    // Eligible experts (task 05)

    private static final String EXPERT_1 = SeedData.EXPERT_1.toString();

    private static final String EXPERT_2 = SeedData.EXPERT_2.toString();

    private static String ids(String... ids) {
        return Arrays.stream(ids).map(id -> "\"" + id + "\"").collect(Collectors.joining(",", "[", "]"));
    }

    private static String withExperts(String eligibleExpertIds) {
        return withPackagesAndExperts("[]", eligibleExpertIds);
    }

    private List<String> eligibleExpertRows(String offeringId) {
        return jdbc.sql("select expert_id::text from eligible_expert where consultation_offering_id = :id order by 1")
                .param("id", UUID.fromString(offeringId)).query(String.class).list();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void createsAnOfferingWithEligibleExpertsSortedAndCountedOnce() throws Exception {
        MvcTestResult created = post(withExperts(ids(EXPERT_2, EXPERT_1, EXPERT_2)));

        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.eligibleExpertIds").asArray()
                .containsExactly(EXPERT_1, EXPERT_2);
        String id = json(created, "$.id");
        assertThat(get(id).exchange().getResponse().getContentAsString())
                .isEqualTo(created.getResponse().getContentAsString());
        assertThat(eligibleExpertRows(id)).containsExactly(EXPERT_1, EXPERT_2);
        assertThat(list()).bodyJson().extractingPath("$[0].eligibleExpertCount").isEqualTo(2);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void anEditReplacesTheEligibleExpertsAndLeavesAKeptRowAlone() throws Exception {
        String id = json(post(withExperts(ids(EXPERT_1, EXPERT_2))), "$.id");
        OffsetDateTime keptBefore = expertRowUpdatedAt(id, EXPERT_2);

        MvcTestResult edited = put(id, withExperts(ids(EXPERT_2)));

        assertThat(edited).hasStatusOk().bodyJson().extractingPath("$.eligibleExpertIds").asArray()
                .containsExactly(EXPERT_2);
        assertThat(eligibleExpertRows(id)).containsExactly(EXPERT_2);
        assertThat(expertRowUpdatedAt(id, EXPERT_2)).isEqualTo(keptBefore);
        assertThat(get(id)).bodyJson().extractingPath("$.eligibleExpertIds").asArray().containsExactly(EXPERT_2);

        // swapped: the dropped expert comes back as a new row, in the same save as the other one leaves
        assertThat(put(id, withExperts(ids(EXPERT_1)))).hasStatusOk();
        assertThat(eligibleExpertRows(id)).containsExactly(EXPERT_1);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void anEditMayRemoveEveryEligibleExpert() throws Exception {
        String id = json(post(withExperts(ids(EXPERT_1, EXPERT_2))), "$.id");

        assertThat(put(id, withExperts("[]"))).hasStatusOk()
                .bodyJson().extractingPath("$.eligibleExpertIds").asArray().isEmpty();

        assertThat(eligibleExpertRows(id)).isEmpty();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersAnIdThatIsNoExpertWithBadRequestAndChangesNothing() throws Exception {
        String id = json(post(withExperts(ids(EXPERT_1))), "$.id");
        for (String notAnExpert : List.of(SeedData.CUSTOMER_1.toString(), SeedData.ADMIN_1.toString(), UNKNOWN_ID)) {
            // one valid expert next to it: the whole save fails
            assertThat(put(id, withExperts(ids(EXPERT_2, notAnExpert))))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "User %s is not an expert.",
                              "instance": "%s/%s"
                            }
                            """.formatted(notAnExpert, PATH, id));
            assertThat(post(withExperts(ids(notAnExpert)))).hasStatus(HttpStatus.BAD_REQUEST);
        }

        assertThat(eligibleExpertRows(id)).containsExactly(EXPERT_1);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Agile Coaching", "Career Coaching");
    }

    @Test
    @Sql("/com/cleanlearning/api/user/users-that-are-not-offered-as-experts.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    @Sql(scripts = "/com/cleanlearning/api/user/delete-users-that-are-not-offered-as-experts.sql",
            executionPhase = AFTER_TEST_METHOD)
    void answersAnExpertWithoutProfileAndAProfileWithoutTheRoleWithBadRequest() {
        // Without the profile the foreign key would answer 500; without the role the user isn't an expert.
        assertThat(post(withExperts(ids("00000000-0000-0000-0000-000000009101"))))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(withExperts(ids("00000000-0000-0000-0000-000000009102"))))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void answersAMissingOrMalformedExpertIdWithBadRequest() {
        assertThat(post(withExperts("[null]")))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                        error("eligibleExpertIds[0]", "must not be null")));
        assertThat(post(withExperts("[\"not-a-uuid\"]"))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(list()).bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
    }

    @Test
    @Sql(SHOP + "more-published-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void theShopListsTheExpertsTheAdminChoseAndKeepsAnOfferingWithoutExperts() {
        String agileCoaching = "00000000-0000-0000-0000-000000009003";
        String packages = "[" + aPackage("00000000-0000-0000-0000-000000009031", 1, "250.00") + ","
                + aPackage("00000000-0000-0000-0000-000000009032", 4, "900.00") + "]";

        assertThat(put(agileCoaching, withPackagesAndExperts(packages, ids(EXPERT_2)))).hasStatusOk();
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", agileCoaching))
                .hasStatusOk()
                .bodyJson().extractingPath("$.consultation.eligibleExperts[*].displayName").asArray()
                .containsExactly("Expert 2");

        // The shop needs a package, not an expert: with every expert removed, the offering stays.
        assertThat(put(agileCoaching, withPackagesAndExperts(packages, "[]"))).hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[*].name").asArray()
                .containsExactly("Agile Coaching", "Ärger-Coaching", "Career Coaching");
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", agileCoaching))
                .hasStatusOk()
                .bodyJson().extractingPath("$.consultation.eligibleExperts").asArray().isEmpty();
    }

    private OffsetDateTime expertRowUpdatedAt(String offeringId, String expertId) {
        return jdbc.sql("select updated_at from eligible_expert where consultation_offering_id = :id "
                + "and expert_id = :expertId")
                .param("id", UUID.fromString(offeringId)).param("expertId", UUID.fromString(expertId))
                .query((row, rowNumber) -> row.getObject("updated_at", OffsetDateTime.class)).single();
    }

    /** The timestamps of the offering's package rows, fewest sessions first. */
    private List<Timestamps> packageTimestamps(String id) {
        return jdbc.sql("""
                select created_at, updated_at from consultation_package
                where consultation_offering_id = :id order by session_count, id
                """)
                .param("id", UUID.fromString(id))
                .query((row, rowNumber) -> new Timestamps(row.getObject("created_at", OffsetDateTime.class),
                        row.getObject("updated_at", OffsetDateTime.class)))
                .list();
    }

    /** The timestamps of the offering's {@code offering} and {@code consultation_offering} rows. */
    private List<Timestamps> timestamps(String id) {
        return jdbc.sql("""
                select 1 as position, created_at, updated_at from offering where id = :id
                union all
                select 2, created_at, updated_at from consultation_offering where offering_id = :id
                order by position
                """)
                .param("id", UUID.fromString(id))
                .query((row, rowNumber) -> new Timestamps(row.getObject("created_at", OffsetDateTime.class),
                        row.getObject("updated_at", OffsetDateTime.class)))
                .list();
    }

    private record Timestamps(OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }

    private static Map<String, String> error(String field, String message) {
        return Map.of("field", field, "message", message);
    }
}
