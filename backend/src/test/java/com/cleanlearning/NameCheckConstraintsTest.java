package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The check constraints of {@code V3__check_names_not_blank.sql} reject every name that {@code
 * OfferingName} and {@code DisplayName} reject, so a blank name can't be stored and then break the
 * list that reads it. They do so only under the columns' ICU collation: under another collation,
 * Postgres's {@code \s} misses some of these characters.
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
}
