package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Every table has {@code updated_at}, and a before update trigger sets it with {@code
 * set_updated_at()} ({@code V4__set_updated_at.sql}, D-20). A table created later adds the trigger
 * in its create migration; this test fails if it doesn't.
 */
@IntegrationTest
class UpdatedAtTriggersTest {

  @Autowired JdbcClient jdbc;

  @Test
  void everyTableSetsUpdatedAtOnUpdate() {
    List<String> tables =
        jdbc.sql(
                """
                select table_name from information_schema.tables
                where table_schema = current_schema() and table_type = 'BASE TABLE'
                  and table_name <> 'flyway_schema_history'
                """)
            .query(String.class)
            .list();
    List<String> tablesWithTrigger =
        jdbc.sql(
                """
                select c.table_name from information_schema.columns c
                join information_schema.triggers t
                  on t.event_object_schema = c.table_schema and t.event_object_table = c.table_name
                where c.table_schema = current_schema() and c.column_name = 'updated_at'
                  and t.event_manipulation = 'UPDATE' and t.action_timing = 'BEFORE'
                  and t.action_orientation = 'ROW' and t.action_statement = 'EXECUTE FUNCTION set_updated_at()'
                """)
            .query(String.class)
            .list();

    assertThat(tables).isNotEmpty();
    assertThat(tablesWithTrigger).containsExactlyInAnyOrderElementsOf(tables);
  }
}
