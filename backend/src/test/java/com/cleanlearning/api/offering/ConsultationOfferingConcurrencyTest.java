package com.cleanlearning.api.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.domain.user.value.Role;

/**
 * The offering lock (D-27): two status changes on one offering at once can't both pass. Without
 * {@code LockConsultationOfferingPort}'s row lock, both requests would read {@code PUBLISHED}, and both would succeed.
 * <p>
 * To make the race certain rather than likely, the test holds the offering's row lock itself while both requests
 * start, waits until Postgres shows both of them waiting for a lock, and only then lets go. With the lock, both wait
 * at the lock and then run one after the other. Without it, both read the offering at once and wait only at their
 * update, and both pass.
 */
@IntegrationTest
class ConsultationOfferingConcurrencyTest {

    // Agile Coaching from the shop tests' script: published, with two packages
    private static final UUID AGILE_COACHING = UUID.fromString("00000000-0000-0000-0000-000000009003");

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcClient jdbc;

    @Test
    @Sql("/com/cleanlearning/api/shop/more-published-offerings.sql")
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void archiveAndUnpublishAtOnceCantBothPass() throws Exception {
        ExecutorService requests = Executors.newFixedThreadPool(2);
        List<Integer> statuses;
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement lock = connection.prepareStatement(
                    "select id from offering where id = ? for no key update")) {
                lock.setObject(1, AGILE_COACHING);
                lock.executeQuery().close();
            }

            Future<Integer> archive = requests.submit(() -> change("archive"));
            Future<Integer> unpublish = requests.submit(() -> change("unpublish"));
            awaitTransactionsWaitingForALock(2);
            connection.commit();

            statuses = List.of(archive.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS),
                    unpublish.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
        } finally {
            requests.shutdownNow();
            // A failed run waits for its requests, so they can't race the cleanup script.
            requests.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        }

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        String status = jdbc.sql("select status from offering where id = :id").param("id", AGILE_COACHING)
                .query(String.class).single();
        // archive first: unpublishing an archived offering is refused; unpublish first: archiving a draft is
        assertThat(status).isEqualTo(statuses.get(0) == 200 ? "ARCHIVED" : "DRAFT");
    }

    private int change(String action) {
        return mockMvc.post().uri("/api/admin/consultation-offerings/{id}/" + action, AGILE_COACHING)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .exchange()
                .getResponse().getStatus();
    }

    private void awaitTransactionsWaitingForALock(int count) throws InterruptedException {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (waitingForALock() < count) {
            assertThat(Instant.now()).as("%d transactions waiting for a lock", count).isBefore(deadline);
            Thread.sleep(10);
        }
    }

    /** This test's statements waiting for a lock: those on the {@code offering} table, a word in their text. */
    private long waitingForALock() {
        return jdbc.sql("""
                select count(*) from pg_stat_activity
                where datname = current_database() and wait_event_type = 'Lock' and query ~ '\\moffering\\M'
                """).query(Long.class).single();
    }
}
