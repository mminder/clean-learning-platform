package com.cleanlearning.api.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestClock;
import com.cleanlearning.domain.user.value.Role;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * UC-13: an expert sets their availability and sees it again. Tests save availability through the
 * API; {@link SeedData#DELETE_TEST_DATA} removes it afterwards. "Now" is Mon 9 Nov 2026, 15:00 in
 * Zurich, as in spec 04.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class AvailabilityControllerTest {

  private static final String PATH = "/api/expert/availability";

  /** Mon 9 Nov 2026, 15:00 in Zurich (UTC+1). */
  private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

  private static final String OVERLAPS = "must not overlap or touch another time range";

  private static final String NOT_AFTER_START = "must be after the start";

  private static final String TWO_DATES_TWO_RANGES =
      """
      {
        "dates": ["2026-11-17", "2026-11-18"],
        "timeRanges": [
          { "start": "09:00", "end": "12:00" },
          { "start": "13:00", "end": "17:00" }
        ]
      }
      """;

  @Autowired MockMvcTester mockMvc;

  @Autowired TestClock clock;

  @Autowired JdbcClient jdbc;

  @BeforeEach
  void setNow() {
    clock.set(NOW);
  }

  private MvcTestResult save(UUID expert, String body) {
    return mockMvc
        .put()
        .uri(PATH)
        .with(LoggedIn.as(expert, Role.EXPERT))
        .with(Csrf.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .exchange();
  }

  private MvcTestResult get(UUID expert) {
    return mockMvc.get().uri(PATH).with(LoggedIn.as(expert, Role.EXPERT)).exchange();
  }

  /** The expert's stored dates, all of them, also the ones before today. */
  private List<LocalDate> storedDates(UUID expert) {
    return jdbc.sql(
            "select available_on from availability_date where expert_id = ? order by available_on")
        .param(expert)
        .query(LocalDate.class)
        .list();
  }

  private int storedTimeRanges(UUID expert) {
    return jdbc.sql("select count(*) from availability_time_range where expert_id = ?")
        .param(expert)
        .query(Integer.class)
        .single();
  }

  /**
   * The request body with these dates, each as JSON (`"2026-11-17"` or `null`), and these ranges.
   */
  private static String availability(List<String> dates, String... ranges) {
    return "{ \"dates\": ["
        + String.join(", ", dates)
        + "], \"timeRanges\": ["
        + String.join(", ", ranges)
        + "] }";
  }

  private static String date(String date) {
    return "\"" + date + "\"";
  }

  private static String range(String start, String end) {
    return "{ \"start\": \"" + start + "\", \"end\": \"" + end + "\" }";
  }

  private static Map<String, String> error(String field, String message) {
    return Map.of("field", field, "message", message);
  }

  /** A 400 with these invalid fields. */
  private void assertRefused(String body, Map<?, ?>... errors) {
    assertThat(save(SeedData.EXPERT_2, body))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .extractingPath("$.errors")
        .isEqualTo(List.of(errors));
  }

  @Test
  void savesTheAvailabilityAndAnswersIt() {
    String expected =
        """
        {
          "dates": ["2026-11-17", "2026-11-18"],
          "timeRanges": [
            { "start": "09:00:00", "end": "12:00:00" },
            { "start": "13:00:00", "end": "17:00:00" }
          ]
        }
        """;

    assertThat(save(SeedData.EXPERT_2, TWO_DATES_TWO_RANGES))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(expected);
    assertThat(get(SeedData.EXPERT_2)).hasStatusOk().bodyJson().isStrictlyEqualTo(expected);
  }

  @Test
  void answersNothingWithoutASavedAvailability() {
    assertThat(get(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            { "dates": [], "timeRanges": [] }
            """);
  }

  @Test
  void answersTheDatesAscendingAndTheRangesByStart() {
    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(
                    List.of(date("2026-11-18"), date("2026-11-09"), date("2026-11-17")),
                    range("13:00", "17:00"),
                    range("09:00", "12:00"),
                    range("07:15", "08:45"))))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "dates": ["2026-11-09", "2026-11-17", "2026-11-18"],
              "timeRanges": [
                { "start": "07:15:00", "end": "08:45:00" },
                { "start": "09:00:00", "end": "12:00:00" },
                { "start": "13:00:00", "end": "17:00:00" }
              ]
            }
            """);
  }

  @Test
  void countsADateNamedTwiceOnce() {
    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(
                    List.of(date("2026-11-17"), date("2026-11-17")), range("09:00", "12:00"))))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.dates")
        .isEqualTo(List.of("2026-11-17"));
    assertThat(storedDates(SeedData.EXPERT_2)).containsExactly(LocalDate.of(2026, 11, 17));
  }

  @Test
  void savesEmptyListsAsEmpty() {
    assertThat(save(SeedData.EXPERT_2, TWO_DATES_TWO_RANGES)).hasStatusOk();

    assertThat(save(SeedData.EXPERT_2, availability(List.of(date("2026-11-17")))))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            { "dates": ["2026-11-17"], "timeRanges": [] }
            """);
    assertThat(save(SeedData.EXPERT_2, availability(List.of())))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            { "dates": [], "timeRanges": [] }
            """);
    assertThat(get(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            { "dates": [], "timeRanges": [] }
            """);
    assertThat(storedDates(SeedData.EXPERT_2)).isEmpty();
    assertThat(storedTimeRanges(SeedData.EXPERT_2)).isZero();
  }

  /**
   * A day later, the save replaces the dates from the new today on, today included, and keeps the
   * earlier ones, which the page no longer shows.
   */
  @Test
  void keepsTheDatesBeforeTodayAndReplacesTheOthers() {
    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(
                    List.of(date("2026-11-09"), date("2026-11-10"), date("2026-11-17")),
                    range("09:00", "12:00"))))
        .hasStatusOk();

    clock.set(NOW.plus(Duration.ofDays(1)));
    String saved =
        """
        { "dates": ["2026-11-18"], "timeRanges": [{ "start": "13:00:00", "end": "17:00:00" }] }
        """;
    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(List.of(date("2026-11-18")), range("13:00", "17:00"))))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(saved);

    assertThat(get(SeedData.EXPERT_2)).hasStatusOk().bodyJson().isStrictlyEqualTo(saved);
    assertThat(storedDates(SeedData.EXPERT_2))
        .containsExactly(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 18));
    assertThat(storedTimeRanges(SeedData.EXPERT_2)).isEqualTo(1);
  }

  @Test
  void refusesADateBeforeToday() {
    assertThat(save(SeedData.EXPERT_2, TWO_DATES_TWO_RANGES)).hasStatusOk();

    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(
                    List.of(date("2026-11-08"), date("2026-11-19")), range("10:00", "11:00"))))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "A date before today can't be changed: 2026-11-08.",
              "instance": "/api/expert/availability"
            }
            """);
    assertThat(storedDates(SeedData.EXPERT_2))
        .containsExactly(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18));
    assertThat(storedTimeRanges(SeedData.EXPERT_2)).isEqualTo(2);
  }

  /**
   * Today is Zurich's, not the clock's zone's: at 00:30 on Tuesday in Zurich it's still Monday in
   * New York, where the clock is.
   */
  @Test
  void takesTodayInZurich() {
    clock.set(Instant.parse("2026-11-09T23:30:00Z"), ZoneId.of("America/New_York"));

    assertThat(save(SeedData.EXPERT_2, availability(List.of(date("2026-11-09")))))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("A date before today can't be changed: 2026-11-09.");
    assertThat(save(SeedData.EXPERT_2, availability(List.of(date("2026-11-10"))))).hasStatusOk();
  }

  @Test
  void refusesARangeThatEndsBeforeItStarts() {
    assertThat(save(SeedData.EXPERT_2, TWO_DATES_TWO_RANGES)).hasStatusOk();

    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(List.of(date("2026-11-17")), range("12:00", "09:00"))))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyJson()
        .isStrictlyEqualTo(
            """
            {
              "title": "Bad Request",
              "status": 400,
              "detail": "Invalid request content.",
              "instance": "/api/expert/availability",
              "errors": [{ "field": "timeRanges[0].end", "message": "must be after the start" }]
            }
            """);
    assertRefused(
        availability(List.of(date("2026-11-17")), range("09:00", "12:00"), range("13:00", "13:00")),
        error("timeRanges[1].end", NOT_AFTER_START));
    assertThat(storedDates(SeedData.EXPERT_2)).hasSize(2);
    assertThat(storedTimeRanges(SeedData.EXPERT_2)).isEqualTo(2);
  }

  @Test
  void refusesOverlappingAndTouchingRangesAtTheLaterOne() {
    assertThat(save(SeedData.EXPERT_2, TWO_DATES_TWO_RANGES)).hasStatusOk();
    List<String> dates = List.of(date("2026-11-19"));

    assertRefused(
        availability(dates, range("09:00", "12:00"), range("11:00", "14:00")),
        error("timeRanges[1].start", OVERLAPS));
    assertRefused(
        availability(dates, range("09:00", "12:00"), range("12:00", "15:00")),
        error("timeRanges[1].start", OVERLAPS));
    // the later-starting one, wherever it is in the list
    assertRefused(
        availability(dates, range("12:00", "15:00"), range("09:00", "12:00")),
        error("timeRanges[0].start", OVERLAPS));
    // the same start twice: the second one
    assertRefused(
        availability(dates, range("09:00", "12:00"), range("09:00", "10:00")),
        error("timeRanges[1].start", OVERLAPS));
    // each invalid range; an inverted one overlaps nothing
    assertRefused(
        availability(
            dates,
            range("09:00", "17:00"),
            range("10:00", "11:00"),
            range("14:00", "10:00"),
            range("16:00", "18:00")),
        error("timeRanges[1].start", OVERLAPS),
        error("timeRanges[2].end", NOT_AFTER_START),
        error("timeRanges[3].start", OVERLAPS));
    assertThat(storedDates(SeedData.EXPERT_2))
        .containsExactly(LocalDate.of(2026, 11, 17), LocalDate.of(2026, 11, 18));
    assertThat(get(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.timeRanges[*].start")
        .isEqualTo(List.of("09:00:00", "13:00:00"));
  }

  @Test
  void acceptsRangesWithAGapOfAMinute() {
    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(
                    List.of(date("2026-11-17")), range("09:00", "12:00"), range("12:01", "15:00"))))
        .hasStatusOk();
  }

  @Test
  void answersMissingValuesWithEachField() {
    assertRefused(
        "{}", error("dates", "must not be null"), error("timeRanges", "must not be null"));
    assertRefused(
        availability(List.of("null"), "null", "{}"),
        error("dates[0]", "must not be null"),
        error("timeRanges[0]", "must not be null"),
        error("timeRanges[1].end", "must not be null"),
        error("timeRanges[1].start", "must not be null"));
    // an empty time input of the page
    assertRefused(
        availability(List.of(), range("", "")),
        error("timeRanges[0].end", "must not be null"),
        error("timeRanges[0].start", "must not be null"));
    assertThat(storedDates(SeedData.EXPERT_2)).isEmpty();
  }

  @Test
  void answersAMalformedTimeWithBadRequest() {
    assertThat(
            save(
                SeedData.EXPERT_2,
                availability(List.of(date("2026-11-17")), range("9am", "12:00"))))
        .hasStatus(HttpStatus.BAD_REQUEST)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(storedDates(SeedData.EXPERT_2)).isEmpty();
  }

  @Test
  void answersABodyThatIsNotJsonWithUnsupportedMediaType() {
    assertThat(
            mockMvc
                .put()
                .uri(PATH)
                .with(LoggedIn.as(SeedData.EXPERT_2, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.TEXT_PLAIN)
                .content(TWO_DATES_TWO_RANGES))
        .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    assertThat(storedDates(SeedData.EXPERT_2)).isEmpty();
  }

  @Test
  void eachExpertSeesOnlyTheirOwnAvailability() {
    assertThat(
            save(
                SeedData.EXPERT_1,
                availability(List.of(date("2026-11-20")), range("08:00", "10:00"))))
        .hasStatusOk();
    assertThat(save(SeedData.EXPERT_2, TWO_DATES_TWO_RANGES)).hasStatusOk();

    assertThat(get(SeedData.EXPERT_1))
        .hasStatusOk()
        .bodyJson()
        .isStrictlyEqualTo(
            """
            { "dates": ["2026-11-20"], "timeRanges": [{ "start": "08:00:00", "end": "10:00:00" }] }
            """);
    assertThat(get(SeedData.EXPERT_2))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.dates")
        .isEqualTo(List.of("2026-11-17", "2026-11-18"));

    // Expert 2's save doesn't touch Expert 1's availability.
    assertThat(save(SeedData.EXPERT_2, availability(List.of()))).hasStatusOk();
    assertThat(storedDates(SeedData.EXPERT_1)).containsExactly(LocalDate.of(2026, 11, 20));
    assertThat(storedTimeRanges(SeedData.EXPERT_1)).isEqualTo(1);
  }
}
