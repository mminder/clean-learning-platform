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

/**
 * UC-15: the assigned expert adds, edits and deletes notes on a case, and sees them on the case
 * detail, newest first. Tests save availability, buy and book through the API; {@link
 * SeedData#DELETE_TEST_DATA} removes it all afterwards, the notes first. "Now" is Mon 9 Nov 2026,
 * 15:00 in Zurich, as in spec 06, unless a test moves the clock. In November, Zurich is UTC+1:
 * 15:00 in Zurich is 14:00Z.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class ConsultationCaseNotesTest {

  private static final String PATH = "/api/expert/consultation-cases";

  private static final String CUSTOMER_CASES = "/api/customer/consultation-cases";

  /** Mon 9 Nov 2026, 15:00 in Zurich. */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  /** Tue 10 Nov 2026, 09:00 in Zurich. */
  private static final Instant NEXT_MORNING = Instant.parse("2026-11-10T08:00:00Z");

  private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

  /** Tue 17 Nov 2026, 09:00, 10:00 and 11:00 in Zurich. */
  private static final String NINE = "2026-11-17T08:00:00Z";

  private static final String TEN = "2026-11-17T09:00:00Z";
  private static final String ELEVEN = "2026-11-17T10:00:00Z";

  private static final String FIRST_SESSION =
      """
      { "title": "First session", "text": "Line one\\nLine two", "visibility": "PRIVATE" }
      """;

  @Autowired MockMvcTester mockMvc;

  @Autowired TestClock clock;

  @Autowired JdbcClient jdbc;

  /** Customer 1 buys Career Coaching and books Expert 2 at 10:00 on 17 Nov; returns the case. */
  private String customer1sCaseWithExpert2() throws Exception {
    return customer1sCaseWithExpert2On(TEN);
  }

  /** Customer 1 buys Career Coaching and books Expert 2 at this start; returns the case's id. */
  private String customer1sCaseWithExpert2On(String start) throws Exception {
    return caseOf(SeedData.CUSTOMER_1, SeedData.EXPERT_2, start);
  }

  /** The customer buys Career Coaching and books the expert at this start; returns the case. */
  private String caseOf(UUID customer, UUID expert, String start) throws Exception {
    String caseId = boughtCareerCoaching(customer);
    booked(caseId, expert, start, customer);
    return caseId;
  }

  /** Now, Expert 2 is available on 17 and 24 Nov, 09:00–12:00. */
  @BeforeEach
  void setNowAndAvailability() {
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
                    {
                      "dates": ["2026-11-17", "2026-11-24"],
                      "timeRanges": [{ "start": "09:00", "end": "12:00" }]
                    }
                    """))
        .hasStatusOk();
  }

  /** The customer buys Career Coaching's 3 sessions; returns the new case's id. */
  private String boughtCareerCoaching(UUID customer) throws Exception {
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
    return JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
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

  private MvcTestResult add(Object caseId, String body, UUID expert) {
    return mockMvc
        .post()
        .uri(PATH + "/{id}/notes", caseId)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .with(Csrf.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  /** Expert 2 adds the note; returns its id. */
  private String added(String caseId, String body) throws Exception {
    MvcTestResult added = add(caseId, body, SeedData.EXPERT_2);
    assertThat(added).hasStatus(HttpStatus.CREATED);
    return JsonPath.read(added.getResponse().getContentAsString(), "$.id");
  }

  private MvcTestResult edit(Object caseId, Object noteId, String body, UUID expert) {
    return mockMvc
        .put()
        .uri(PATH + "/{id}/notes/{noteId}", caseId, noteId)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .with(Csrf.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private MvcTestResult delete(Object caseId, Object noteId, UUID expert) {
    return mockMvc
        .delete()
        .uri(PATH + "/{id}/notes/{noteId}", caseId, noteId)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .with(Csrf.token())
        .exchange();
  }

  private static String note(String title, String text, String visibility) {
    return """
    { "title": "%s", "text": "%s", "visibility": "%s" }
    """
        .formatted(title, text, visibility);
  }

  private static Map<String, String> error(String field, String message) {
    return Map.of("field", field, "message", message);
  }

  /** Every note's title, text, visibility and times, as the database holds them. */
  private List<Map<String, Object>> storedNotes() {
    return jdbc.sql(
            "select id, title, text, visibility, written_at, edited_at from note order by id")
        .query()
        .listOfRows();
  }

  private MvcTestResult getCase(Object caseId, UUID expert) {
    return mockMvc
        .get()
        .uri(PATH + "/{id}", caseId)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .exchange();
  }

  /** UC-15 steps 1 to 4, spec 06's scenario "Add a private note". */
  @Test
  void addsANoteWithTheTimeItWasWritten() throws Exception {
    String caseId = customer1sCaseWithExpert2();

    MvcTestResult added = add(caseId, FIRST_SESSION, SeedData.EXPERT_2);

    assertThat(added).hasStatus(HttpStatus.CREATED);
    String noteId = JsonPath.read(added.getResponse().getContentAsString(), "$.id");
    assertThat(added)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "id": "%s",
              "title": "First session",
              "text": "Line one\\nLine two",
              "visibility": "PRIVATE",
              "writtenAt": "2026-11-09T14:00:00Z",
              "editedAt": null
            }
            """
                .formatted(noteId));
    assertThat(getCase(caseId, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "notes": [{
                "id": "%s",
                "title": "First session",
                "text": "Line one\\nLine two",
                "visibility": "PRIVATE",
                "writtenAt": "2026-11-09T14:00:00Z",
                "editedAt": null
              }]
            }
            """
                .formatted(noteId));
    assertThat(getCase(caseId, SeedData.EXPERT_2))
        .bodyJson()
        .extractingPath("$.notes[*].id")
        .asList()
        .containsExactly(noteId);
  }

  /**
   * UC-15's alternative flow "Edit", spec 06's scenario "Edit a note": the edit answers the note
   * with its {@code editedAt} from the clock, and the note keeps its place in the list.
   */
  @Test
  void editsANoteWhichKeepsItsPlace() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String first = added(caseId, FIRST_SESSION);
    clock.set(NOW.plusSeconds(600));
    String second = added(caseId, note("Homework", "Read chapter 3", "PUBLIC"));
    clock.set(NEXT_MORNING);

    MvcTestResult edited =
        edit(
            caseId,
            first,
            note("First session", "Line one\\nLine three", "PUBLIC"),
            SeedData.EXPERT_2);

    assertThat(edited)
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "id": "%s",
              "title": "First session",
              "text": "Line one\\nLine three",
              "visibility": "PUBLIC",
              "writtenAt": "2026-11-09T14:00:00Z",
              "editedAt": "2026-11-10T08:00:00Z"
            }
            """
                .formatted(first));
    assertThat(getCase(caseId, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.notes[*].id")
        .asList()
        .containsExactly(second, first);
  }

  /** Spec 06, decision 5: a change of visibility alone is an edit, and doesn't move the note. */
  @Test
  void anEditOfTheVisibilityAloneIsAnEditToo() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String older = added(caseId, FIRST_SESSION);
    clock.set(NOW.plusSeconds(600));
    String newer = added(caseId, note("Homework", "Read chapter 3", "PUBLIC"));
    clock.set(NEXT_MORNING);

    assertThat(
            edit(caseId, newer, note("Homework", "Read chapter 3", "PRIVATE"), SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "visibility": "PRIVATE", "writtenAt": "2026-11-09T14:10:00Z", "editedAt": "2026-11-10T08:00:00Z" }
            """);
    assertThat(edit(caseId, older, FIRST_SESSION, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.editedAt")
        .isEqualTo("2026-11-10T08:00:00Z");
    assertThat(getCase(caseId, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.notes[*].id")
        .asList()
        .containsExactly(newer, older);
  }

  /** UC-15's alternative flow "Delete": for good, so a second delete finds nothing. */
  @Test
  void deletesANote() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String noteId = added(caseId, FIRST_SESSION);

    assertThat(delete(caseId, noteId, SeedData.EXPERT_2)).hasStatus(HttpStatus.NO_CONTENT);

    assertThat(getCase(caseId, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.notes")
        .asList()
        .isEmpty();
    assertThat(storedNotes()).isEmpty();
    assertThat(delete(caseId, noteId, SeedData.EXPERT_2))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Not Found",
              "status": 404,
              "detail": "Note %s not found.",
              "instance": "/api/expert/consultation-cases/%s/notes/%s"
            }
            """
                .formatted(noteId, caseId, noteId));
  }

  /** Spec 06's scenario "Two tabs, one note": saving a note that's gone answers 404. */
  @Test
  void editingADeletedNoteIsNotFound() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String noteId = added(caseId, FIRST_SESSION);
    assertThat(delete(caseId, noteId, SeedData.EXPERT_2)).hasStatus(HttpStatus.NO_CONTENT);

    assertThat(edit(caseId, noteId, FIRST_SESSION, SeedData.EXPERT_2))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Note %s not found.".formatted(noteId));
    assertThat(storedNotes()).isEmpty();
  }

  /**
   * UC-15's alternative flow "Missing title or text": a title or a text that's missing, empty or
   * only spaces, and a missing visibility, each named, and nothing is saved.
   */
  @Test
  void anInvalidNoteNamesEachFieldAndNothingIsSaved() throws Exception {
    String caseId = customer1sCaseWithExpert2();

    assertThat(add(caseId, "{}", SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Invalid request content.",
              "instance": "%s/%s/notes",
              "errors": [
                { "field": "text", "message": "must not be blank" },
                { "field": "title", "message": "must not be blank" },
                { "field": "visibility", "message": "must not be null" }
              ]
            }
            """
                .formatted(PATH, caseId));
    assertThat(add(caseId, note("", "  ", "PRIVATE"), SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors")
        .isEqualTo(
            List.of(error("text", "must not be blank"), error("title", "must not be blank")));
    // no-break spaces: Bean Validation lets them through, the value object answers 400
    assertThat(add(caseId, note("\u00A0", "Text", "PRIVATE"), SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "status": 400, "detail": "A note's title is not blank." }
            """);
    assertThat(add(caseId, note("Title", "\u00A0\\n\u2007", "PRIVATE"), SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("A note's text is not blank.");
    // not a visibility: unreadable, without errors
    assertThat(add(caseId, note("Title", "Text", "SECRET"), SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(storedNotes()).isEmpty();

    String noteId = added(caseId, FIRST_SESSION);
    List<Map<String, Object>> stored = storedNotes();
    assertThat(edit(caseId, noteId, note(" ", "Text", "PUBLIC"), SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors")
        .isEqualTo(List.of(error("title", "must not be blank")));
    assertThat(
            edit(
                caseId,
                noteId,
                """
                { "title": "First session", "text": "Text" }
                """,
                SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors")
        .isEqualTo(List.of(error("visibility", "must not be null")));
    assertThat(edit(caseId, noteId, note("Title", "\u00A0", "PUBLIC"), SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("A note's text is not blank.");
    assertThat(storedNotes()).isEqualTo(stored);
  }

  /**
   * UC-15's business rule, spec 06's scenario "Notes in any status": an active, a completed and a
   * cancelled case.
   */
  @Test
  void notesCanBeWrittenOnACaseInAnyStatus() throws Exception {
    // Tue 24 Nov 2026, 11:00 in Zurich
    String active = customer1sCaseWithExpert2On("2026-11-24T10:00:00Z");
    String completed = boughtCareerCoaching(SeedData.CUSTOMER_1);
    for (String start : List.of(NINE, TEN, ELEVEN)) {
      booked(completed, SeedData.EXPERT_2, start, SeedData.CUSTOMER_1);
    }
    String cancelled = customer1sCaseWithExpert2On("2026-11-24T09:00:00Z");
    assertThat(
            mockMvc
                .post()
                .uri(CUSTOMER_CASES + "/{id}/cancel", cancelled)
                .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"refundEligible\": true }"))
        .hasStatusOk();
    // Wed 18 Nov 2026, 10:00 in Zurich: the completed case's appointments have ended
    clock.set(Instant.parse("2026-11-18T09:00:00Z"));
    assertThat(getCase(completed, SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            { "openCredits": 0, "cancelledAt": null, "upcomingAppointments": [] }
            """);

    for (String caseId : List.of(active, completed, cancelled)) {
      String noteId = added(caseId, FIRST_SESSION);
      assertThat(edit(caseId, noteId, note("Edited", "Text", "PUBLIC"), SeedData.EXPERT_2))
          .hasStatusOk();
      assertThat(getCase(caseId, SeedData.EXPERT_2))
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.notes[0].title")
          .isEqualTo("Edited");
      assertThat(delete(caseId, noteId, SeedData.EXPERT_2)).hasStatus(HttpStatus.NO_CONTENT);
    }
    assertThat(storedNotes()).isEmpty();
  }

  /**
   * Security.md's ownership with both seeded experts: Expert 1 on Customer 1's case, which has
   * Expert 2, and a note of another case of Expert 2 under this case's path, are 404s like an
   * unknown id, and nothing changes.
   */
  @Test
  void anotherExpertsCaseAndAnotherCasesNoteAreNotFound() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String noteId = added(caseId, FIRST_SESSION);
    String otherCaseId = customer1sCaseWithExpert2On(ELEVEN);
    String otherNoteId = added(otherCaseId, note("Other case", "Text", "PRIVATE"));
    List<Map<String, Object>> stored = storedNotes();

    assertThat(add(caseId, FIRST_SESSION, SeedData.EXPERT_1))
        .hasStatus(HttpStatus.NOT_FOUND)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Case %s not found.".formatted(caseId));
    assertThat(edit(caseId, noteId, note("Changed", "Text", "PUBLIC"), SeedData.EXPERT_1))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Note %s not found.".formatted(noteId));
    assertThat(delete(caseId, noteId, SeedData.EXPERT_1)).hasStatus(HttpStatus.NOT_FOUND);
    // a note of Expert 2's other case under this case's path
    assertThat(edit(caseId, otherNoteId, note("Changed", "Text", "PUBLIC"), SeedData.EXPERT_2))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Note %s not found.".formatted(otherNoteId));
    assertThat(delete(caseId, otherNoteId, SeedData.EXPERT_2)).hasStatus(HttpStatus.NOT_FOUND);
    // a case's customer who is an expert isn't its assigned expert
    String ofExpert1 = caseOf(SeedData.EXPERT_1, SeedData.EXPERT_2, NINE);
    assertThat(add(ofExpert1, FIRST_SESSION, SeedData.EXPERT_1)).hasStatus(HttpStatus.NOT_FOUND);

    assertThat(storedNotes()).isEqualTo(stored);
    assertThat(getCase(caseId, SeedData.EXPERT_2))
        .bodyJson()
        .extractingPath("$.notes[*].id")
        .asList()
        .containsExactly(noteId);
  }

  @Test
  void answersUnknownAndMalformedIdsWithNotFoundAndBadRequest() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String noteId = added(caseId, FIRST_SESSION);

    assertThat(add(UNKNOWN_ID, FIRST_SESSION, SeedData.EXPERT_2))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Case %s not found.".formatted(UNKNOWN_ID));
    assertThat(edit(caseId, UNKNOWN_ID, FIRST_SESSION, SeedData.EXPERT_2))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Note %s not found.".formatted(UNKNOWN_ID));
    assertThat(edit(UNKNOWN_ID, noteId, FIRST_SESSION, SeedData.EXPERT_2))
        .hasStatus(HttpStatus.NOT_FOUND);
    assertThat(delete(caseId, UNKNOWN_ID, SeedData.EXPERT_2)).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(delete(UNKNOWN_ID, noteId, SeedData.EXPERT_2)).hasStatus(HttpStatus.NOT_FOUND);

    assertThat(add("not-a-uuid", FIRST_SESSION, SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(edit(caseId, "not-a-uuid", FIRST_SESSION, SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(delete(caseId, "not-a-uuid", SeedData.EXPERT_2))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(storedNotes()).hasSize(1);
  }

  private MvcTestResult getCustomerCase(Object caseId, UUID user, Role... roles) {
    return mockMvc
        .get()
        .uri(CUSTOMER_CASES + "/{id}", caseId)
        .with(LoggedIn.as(user, roles))
        .exchange();
  }

  /**
   * Spec 06's scenario "Public notes reach the customer", security.md's shaped responses: the
   * customer's case has the public note, without a visibility, and nothing of the private one, not
   * even its id. Customer 2 still gets 404.
   */
  @Test
  void theCustomerSeesThePublicNotesOnly() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String privateNote = added(caseId, FIRST_SESSION);
    clock.set(NOW.plusSeconds(600));
    String publicNote = added(caseId, note("Homework", "Read chapter 3", "PUBLIC"));
    clock.set(NEXT_MORNING);
    assertThat(
            edit(
                caseId,
                publicNote,
                note("Homework", "Read chapter 3\\nand 4", "PUBLIC"),
                SeedData.EXPERT_2))
        .hasStatusOk();

    MvcTestResult customersCase = getCustomerCase(caseId, SeedData.CUSTOMER_1, Role.CUSTOMER);

    assertThat(customersCase)
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "notes": [{
                "id": "%s",
                "title": "Homework",
                "text": "Read chapter 3\\nand 4",
                "writtenAt": "2026-11-09T14:10:00Z",
                "editedAt": "2026-11-10T08:00:00Z"
              }]
            }
            """
                .formatted(publicNote));
    assertThat(customersCase)
        .bodyJson()
        .extractingPath("$.notes[0]")
        .asMap()
        .containsOnlyKeys("id", "title", "text", "writtenAt", "editedAt");
    assertThat(customersCase.getResponse().getContentAsString())
        .doesNotContain(privateNote)
        .doesNotContain("First session")
        .doesNotContain("Line one")
        .doesNotContain("visibility");
    assertThat(getCustomerCase(caseId, SeedData.CUSTOMER_2, Role.CUSTOMER))
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Case %s not found.".formatted(caseId));
  }

  /** Spec 06's scenario "Edit a note": made public, the customer sees it; private again, not. */
  @Test
  void aNoteMadePublicReachesTheCustomerAndMadePrivateAgainIsGone() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String noteId = added(caseId, FIRST_SESSION);
    assertThat(getCustomerCase(caseId, SeedData.CUSTOMER_1, Role.CUSTOMER))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.notes")
        .asList()
        .isEmpty();
    clock.set(NEXT_MORNING);

    assertThat(edit(caseId, noteId, note("First session", "Text", "PUBLIC"), SeedData.EXPERT_2))
        .hasStatusOk();
    assertThat(getCustomerCase(caseId, SeedData.CUSTOMER_1, Role.CUSTOMER))
        .hasStatusOk()
        .bodyJson()
        .isLenientlyEqualTo(
            """
            {
              "notes": [{
                "id": "%s",
                "title": "First session",
                "text": "Text",
                "writtenAt": "2026-11-09T14:00:00Z",
                "editedAt": "2026-11-10T08:00:00Z"
              }]
            }
            """
                .formatted(noteId));

    assertThat(edit(caseId, noteId, note("First session", "Text", "PRIVATE"), SeedData.EXPERT_2))
        .hasStatusOk();
    MvcTestResult customersCase = getCustomerCase(caseId, SeedData.CUSTOMER_1, Role.CUSTOMER);
    assertThat(customersCase).hasStatusOk().bodyJson().extractingPath("$.notes").asList().isEmpty();
    assertThat(customersCase.getResponse().getContentAsString()).doesNotContain(noteId);
  }

  /**
   * The answers of both cancellations are the customer's case, too: they load it through the same
   * port, with the public notes only.
   */
  @Test
  void bothCancellationsAnswerThePublicNotesOnly() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    booked(caseId, SeedData.EXPERT_2, ELEVEN, SeedData.CUSTOMER_1);
    String privateNote = added(caseId, FIRST_SESSION);
    String publicNote = added(caseId, note("Homework", "Read chapter 3", "PUBLIC"));
    String appointmentId =
        JsonPath.read(
            getCustomerCase(caseId, SeedData.CUSTOMER_1, Role.CUSTOMER)
                .getResponse()
                .getContentAsString(),
            "$.upcomingAppointments[0].id");

    MvcTestResult appointmentCancelled =
        mockMvc
            .post()
            .uri(
                CUSTOMER_CASES + "/{id}/appointments/{appointmentId}/cancel", caseId, appointmentId)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .with(Csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"creditRefunded\": true }")
            .exchange();
    MvcTestResult caseCancelled =
        mockMvc
            .post()
            .uri(CUSTOMER_CASES + "/{id}/cancel", caseId)
            .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
            .with(Csrf.token())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{ \"refundEligible\": true }")
            .exchange();

    for (MvcTestResult answer : List.of(appointmentCancelled, caseCancelled)) {
      assertThat(answer)
          .hasStatusOk()
          .bodyJson()
          .extractingPath("$.notes[*].id")
          .asList()
          .containsExactly(publicNote);
      assertThat(answer.getResponse().getContentAsString()).doesNotContain(privateNote);
    }
  }

  /**
   * Spec 06's scenario "An expert who is also the customer": the customer's view is the customer's,
   * whoever holds it. Expert 1 booked themself and sees only the public note on the customer's
   * route, and both on the expert's.
   */
  @Test
  void anExpertWhoIsTheCustomerSeesThePublicNotesOnlyOnTheCustomersRoute() throws Exception {
    assertThat(
            mockMvc
                .put()
                .uri("/api/expert/availability")
                .with(LoggedIn.as(SeedData.EXPERT_1, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    { "dates": ["2026-11-17"], "timeRanges": [{ "start": "09:00", "end": "12:00" }] }
                    """))
        .hasStatusOk();
    String caseId = caseOf(SeedData.EXPERT_1, SeedData.EXPERT_1, TEN);
    MvcTestResult privateAdded = add(caseId, FIRST_SESSION, SeedData.EXPERT_1);
    assertThat(privateAdded).hasStatus(HttpStatus.CREATED);
    String privateNote = JsonPath.read(privateAdded.getResponse().getContentAsString(), "$.id");
    clock.set(NOW.plusSeconds(600));
    MvcTestResult publicAdded =
        add(caseId, note("Homework", "Read chapter 3", "PUBLIC"), SeedData.EXPERT_1);
    assertThat(publicAdded).hasStatus(HttpStatus.CREATED);
    String publicNote = JsonPath.read(publicAdded.getResponse().getContentAsString(), "$.id");

    MvcTestResult customersCase =
        getCustomerCase(caseId, SeedData.EXPERT_1, Role.EXPERT, Role.CUSTOMER);
    assertThat(customersCase)
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.notes[*].id")
        .asList()
        .containsExactly(publicNote);
    assertThat(customersCase.getResponse().getContentAsString()).doesNotContain(privateNote);
    assertThat(getCase(caseId, SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.notes[*].id")
        .asList()
        .containsExactly(publicNote, privateNote);
  }

  @Test
  void aBodyThatIsNotJsonIsAnUnsupportedMediaType() throws Exception {
    String caseId = customer1sCaseWithExpert2();
    String noteId = added(caseId, FIRST_SESSION);
    List<Map<String, Object>> stored = storedNotes();

    assertThat(
            mockMvc
                .post()
                .uri(PATH + "/{id}/notes", caseId)
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content(FIRST_SESSION))
        .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(
            mockMvc
                .put()
                .uri(PATH + "/{id}/notes/{noteId}", caseId, noteId)
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content(note("Changed", "Text", "PUBLIC")))
        .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(storedNotes()).isEqualTo(stored);
  }
}
