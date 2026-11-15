package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;

/**
 * The check constraints of {@code V3__check_names_not_blank.sql} reject every name that {@code
 * OfferingName} and {@code DisplayName} reject, so a blank name can't be stored and then break the
 * list that reads it, and those of {@code V11__create_note.sql} every title and text that {@code
 * NoteTitle} and {@code NoteText} reject. They do so only under the columns' ICU collation: under
 * another collation, Postgres's {@code \s} misses some of these characters.
 */
@IntegrationTest
class NameCheckConstraintsTest {

  /** Every character that {@code OfferingName} and {@code DisplayName} treat as blank. */
  private static final String BLANK =
      "\t\n\u000B\f\r\u001C\u001D\u001E\u001F \u0085  " + "               　";

  @Autowired JdbcClient jdbc;

  @Test
  void rejectsABlankOfferingName() {
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() ->
            jdbc.sql("update offering set name = ? where id = ?")
                .params(BLANK, SeedData.CAREER_COACHING)
                .update());
  }

  @Test
  void rejectsABlankDisplayName() {
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() ->
            jdbc.sql("update expert_profile set display_name = ? where user_id = ?")
                .params(BLANK, SeedData.EXPERT_1)
                .update());
  }

  @Test
  @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
  void rejectsABlankNoteTitleAndText() {
    UUID caseId = UUID.randomUUID();
    jdbc.sql(
            """
            insert into consultation_case (id, customer_id, consultation_offering_id, price_paid,
                session_count, session_duration_minutes, cancellation_deadline_hours, purchased_at, status)
            values (?, ?, ?, 450, 3, 60, 24, now(), 'ACTIVE')
            """)
        .params(caseId, SeedData.CUSTOMER_1, SeedData.CAREER_COACHING)
        .update();
    String insert =
        """
        insert into note (id, consultation_case_id, title, text, visibility, written_at)
        values (?, ?, ?, ?, 'PRIVATE', now())
        """;

    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(
            () -> jdbc.sql(insert).params(UUID.randomUUID(), caseId, BLANK, "Text").update());
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(
            () -> jdbc.sql(insert).params(UUID.randomUUID(), caseId, "Title", BLANK).update());
  }
}
