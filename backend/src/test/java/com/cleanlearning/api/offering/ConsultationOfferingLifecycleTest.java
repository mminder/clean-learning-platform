package com.cleanlearning.api.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.util.List;
import java.util.UUID;

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
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;

/**
 * UC-16 step 5: publish, unpublish, archive and delete a consultation offering. Like
 * {@link ConsultationOfferingControllerTest}, a test creates its own offerings and {@link SeedData#DELETE_TEST_OFFERINGS}
 * removes them afterwards.
 */
@IntegrationTest
class ConsultationOfferingLifecycleTest {

    private static final String PATH = "/api/admin/consultation-offerings";

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    JdbcClient jdbc;

    private MvcTestResult post(String uri, String body) {
        return mockMvc.post().uri(uri)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult delete(String id) {
        return mockMvc.delete().uri(PATH + "/{id}", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .exchange();
    }

    private MvcTestResult put(String id, String body) {
        return mockMvc.put().uri(PATH + "/{id}", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MockMvcTester.MockMvcRequestBuilder get(String id) {
        return mockMvc.get().uri(PATH + "/{id}", id).with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN));
    }

    private MvcTestResult change(String id, String action) {
        return mockMvc.post().uri(PATH + "/{id}/" + action, id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .exchange();
    }

    private static String draft(String packages, String eligibleExpertIds) {
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

    /** A draft with a package and an eligible expert: complete, so it may be published. */
    private String createCompleteDraft() throws Exception {
        MvcTestResult created = post(PATH, draft("[{ \"sessionCount\": 1, \"price\": \"160\" }]",
                "[\"" + SeedData.EXPERT_2 + "\"]"));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    }

    /** A complete draft, published through the API. */
    private String createPublished() throws Exception {
        String id = createCompleteDraft();
        assertThat(change(id, "publish")).hasStatusOk();
        return id;
    }

    /** A complete offering, published and then archived through the API. */
    private String createArchived() throws Exception {
        String id = createPublished();
        assertThat(change(id, "archive")).hasStatusOk();
        return id;
    }

    private String status(String id) throws Exception {
        return JsonPath.read(get(id).exchange().getResponse().getContentAsString(), "$.status");
    }

    /** A 409 with this code and detail, as the frontend reads it. */
    private static void assertConflict(MvcTestResult result, String code, String detail, String instance) {
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
                        """.formatted(detail, instance, code));
    }

    private static void assertWrongStatus(MvcTestResult result, String detail) {
        assertThat(result).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("WRONG_OFFERING_STATUS");
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(detail);
    }

    private void assertShopLists(String... names) {
        assertThat(mockMvc.get().uri("/api/shop/offerings"))
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly((Object[]) names);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void publishesACompleteDraftAndTheShopListsIt() throws Exception {
        String id = createCompleteDraft();

        assertThat(change(id, "publish"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");

        assertShopLists("Agile Coaching", "Career Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void publishingADraftWithoutAnEligibleExpertAnswersOfferingIncomplete() throws Exception {
        String id = JsonPath.read(post(PATH, draft("[{ \"sessionCount\": 1, \"price\": \"160\" }]", "[]"))
                .getResponse().getContentAsString(), "$.id");

        assertConflict(change(id, "publish"), "OFFERING_INCOMPLETE",
                "The offering needs at least one eligible expert before it can be published.",
                PATH + "/" + id + "/publish");

        assertThat(status(id)).isEqualTo("DRAFT");
        assertShopLists("Career Coaching");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void publishingADraftWithoutAPackageOrAnExpertNamesBoth() throws Exception {
        String id = JsonPath.read(post(PATH, draft("[]", "[]")).getResponse().getContentAsString(), "$.id");

        assertThat(change(id, "publish"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo("The offering needs at least one package and at "
                        + "least one eligible expert before it can be published.");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void publishingAPublishedOfferingAnswersWrongOfferingStatus() throws Exception {
        String id = createPublished();

        assertConflict(change(id, "publish"), "WRONG_OFFERING_STATUS",
                "The offering is published. Only a draft can be published.", PATH + "/" + id + "/publish");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void unpublishesAPublishedOfferingToADraftAndTheShopDropsIt() throws Exception {
        String id = createPublished();

        assertThat(change(id, "unpublish"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("DRAFT");

        assertThat(status(id)).isEqualTo("DRAFT");
        assertShopLists("Career Coaching");
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void unpublishingADraftAnswersWrongOfferingStatus() throws Exception {
        String id = createCompleteDraft();

        assertWrongStatus(change(id, "unpublish"),
                "The offering is a draft. Only a published offering can be unpublished.");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void archivesAPublishedOfferingAndTheShopDropsIt() throws Exception {
        String id = createPublished();

        MvcTestResult archived = change(id, "archive");

        assertThat(archived).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("ARCHIVED");
        // everything else stays
        assertThat(archived).bodyJson().extractingPath("$.packages[*].sessionCount").asArray().containsExactly(1);
        assertThat(archived).bodyJson().extractingPath("$.eligibleExpertIds").asArray()
                .containsExactly(SeedData.EXPERT_2.toString());
        assertThat(status(id)).isEqualTo("ARCHIVED");
        assertShopLists("Career Coaching");
        assertThat(mockMvc.get().uri("/api/shop/offerings/{id}", id)).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void archivingADraftAnswersWrongOfferingStatus() throws Exception {
        String id = createCompleteDraft();

        assertWrongStatus(change(id, "archive"), "The offering is a draft. Only a published offering can be archived.");
        assertThat(status(id)).isEqualTo("DRAFT");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void anArchivedOfferingIsReadOnly() throws Exception {
        String id = createArchived();
        String archived = "The offering is archived and can't be changed any more.";

        // PUT sends everything it keeps, with another name
        assertConflict(put(id, draft("[]", "[]").replace("Agile Coaching", "Lean Coaching")),
                "WRONG_OFFERING_STATUS", archived, PATH + "/" + id);
        assertWrongStatus(change(id, "publish"), archived);
        assertWrongStatus(change(id, "unpublish"), archived);
        assertWrongStatus(change(id, "archive"), archived);
        assertWrongStatus(delete(id), archived);

        assertThat(get(id)).bodyJson().extractingPath("$.name").isEqualTo("Agile Coaching");
        assertThat(status(id)).isEqualTo("ARCHIVED");
        assertThat(get(id)).bodyJson().extractingPath("$.packages").asArray().hasSize(1);
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void deletesADraftWithItsPackagesAndEligibleExperts() throws Exception {
        String id = createCompleteDraft();

        assertThat(delete(id)).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(get(id)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(rows("consultation_package", "consultation_offering_id", id)).isZero();
        assertThat(rows("eligible_expert", "consultation_offering_id", id)).isZero();
        assertThat(rows("consultation_offering", "offering_id", id)).isZero();
        assertThat(rows("offering", "id", id)).isZero();
        assertThat(mockMvc.get().uri(PATH).with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN)))
                .bodyJson().extractingPath("$[*].name").asArray().containsExactly("Career Coaching");
        // the experts themselves stay
        assertThat(rows("expert_profile", "user_id", SeedData.EXPERT_2.toString())).isOne();
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void deletingAPublishedOfferingAnswersWrongOfferingStatus() throws Exception {
        String id = createPublished();

        assertConflict(delete(id), "WRONG_OFFERING_STATUS", "The offering is published. Only a draft can be deleted.",
                PATH + "/" + id);

        assertThat(status(id)).isEqualTo("PUBLISHED");
        assertShopLists("Agile Coaching", "Career Coaching");
    }

    @Test
    void answersUnknownIdsWithNotFound() {
        for (String action : List.of("publish", "unpublish", "archive")) {
            assertThat(change(UNKNOWN_ID, action))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().extractingPath("$.detail").isEqualTo("Offering " + UNKNOWN_ID + " not found.");
        }
        assertThat(delete(UNKNOWN_ID))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void answersMalformedIdsWithBadRequest() {
        assertThat(change("not-a-uuid", "publish")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(delete("not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private long rows(String table, String column, String id) {
        // table and column come from this test, not from a request
        return jdbc.sql("select count(*) from " + table + " where " + column + " = :id")
                .param("id", UUID.fromString(id)).query(Long.class).single();
    }
}
