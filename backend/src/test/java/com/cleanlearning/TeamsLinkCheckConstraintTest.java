package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;

/**
 * The check constraint of {@code V12__expert_teams_link.sql} rejects every link that {@code
 * TeamsLink} rejects (D-36), and accepts the ones it accepts.
 */
@IntegrationTest
@Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
class TeamsLinkCheckConstraintTest {

  @Autowired JdbcClient jdbc;

  private void setLink(String link) {
    jdbc.sql("update expert_profile set teams_link = ? where user_id = ?")
        .params(link, SeedData.EXPERT_2)
        .update();
  }

  @ParameterizedTest
  @ValueSource(strings = {"https://example.com/meet/expert-2", "https://x"})
  void acceptsAnHttpsLink(String link) {
    assertThatCode(() -> setLink(link)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "https://",
        "http://example.com",
        "example.com",
        "HTTPS://example.com",
        " https://example.com",
        "https://example.com/meet expert-2",
        "https://example.com/\tmeet",
        "https://example.com/ meet",
        "https://exämple.com",
        "https://example.com/\n",
        "javascript:alert(1)"
      })
  void rejectsWhatTeamsLinkRejects(String link) {
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() -> setLink(link));
  }
}
