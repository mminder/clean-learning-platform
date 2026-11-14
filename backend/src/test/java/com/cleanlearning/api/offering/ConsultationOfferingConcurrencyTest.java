package com.cleanlearning.api.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.domain.user.value.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The offering lock (D-27): two changes of one offering at once can't both pass, and neither can a
 * purchase and an unpublish. Every change and every purchase locks the offering's row first ({@code
 * LockConsultationOfferingPort}, {@code LockConsultationOfferingForPurchasePort}), so they run one
 * after the other.
 *
 * <p>To make a race certain rather than likely, a test holds the offering's row lock itself while
 * the requests start, waits until Postgres shows them waiting for a lock, and only then lets go.
 * With the lock, they wait at the lock and then run one after the other. Without it, they read the
 * offering at once, and both pass.
 */
@IntegrationTest
class ConsultationOfferingConcurrencyTest {

  // Agile Coaching from the shop tests' script: published, with two packages
  private static final UUID AGILE_COACHING =
      UUID.fromString("00000000-0000-0000-0000-000000009003");

  // its package of 1 session for CHF 250
  private static final UUID AGILE_COACHING_1_SESSION =
      UUID.fromString("00000000-0000-0000-0000-000000009031");

  private static final Duration TIMEOUT = Duration.ofSeconds(10);

  @Autowired MockMvcTester mockMvc;

  @Autowired DataSource dataSource;

  @Autowired JdbcClient jdbc;

  @Test
  @Sql("/com/cleanlearning/api/shop/more-published-offerings.sql")
  @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
  void archiveAndUnpublishAtOnceCantBothPass() throws Exception {
    ExecutorService requests = Executors.newFixedThreadPool(2);
    List<Integer> statuses;
    try (Connection connection = dataSource.getConnection()) {
      lock(connection, "select id from offering where id = ? for no key update");

      Future<Integer> archive = requests.submit(() -> change("archive"));
      Future<Integer> unpublish = requests.submit(() -> change("unpublish"));
      awaitRequestsWaitingForALock(2);
      connection.commit();

      statuses =
          List.of(
              archive.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS),
              unpublish.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
    } finally {
      shutDown(requests);
    }

    assertThat(statuses).containsExactlyInAnyOrder(200, 409);
    // archive first: unpublishing an archived offering is refused; unpublish first: archiving a
    // draft is
    assertThat(status()).isEqualTo(statuses.get(0) == 200 ? "ARCHIVED" : "DRAFT");
  }

  /**
   * The purchase guard (D-07) under the lock. Without the purchase's lock, the purchase reads the
   * offering as published, the unpublish finds no purchase because the purchase hasn't committed
   * yet, and both pass: a draft with a case. So does it if the unpublish asks for purchases before
   * it locks the offering.
   *
   * <p>Besides the offering's row, the test holds its {@code consultation_offering} row, which the
   * new case's foreign key check waits for: so a purchase that has read the offering can't commit
   * until the test lets go. The purchase starts first. With its lock, it waits at the offering,
   * then holds the lock while it waits at the foreign key, and the unpublish waits for it. Without
   * its lock, it waits only at the foreign key, and the unpublish runs and commits first. Only then
   * does the test let go of the foreign key.
   */
  @Test
  @Sql("/com/cleanlearning/api/shop/more-published-offerings.sql")
  @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
  void aPurchaseAndAnUnpublishAtOnceCantBothPass() throws Exception {
    ExecutorService requests = Executors.newFixedThreadPool(2);
    int purchased;
    int unpublished;
    try (Connection offeringLock = dataSource.getConnection();
        Connection foreignKeyLock = dataSource.getConnection()) {
      lock(offeringLock, "select id from offering where id = ? for no key update");
      lock(
          foreignKeyLock,
          "select offering_id from consultation_offering where offering_id = ? for update");
      List<Integer> testBackends = List.of(backend(offeringLock), backend(foreignKeyLock));

      Future<Integer> purchase = requests.submit(this::purchase);
      awaitRequestsWaitingForALock(1);
      Future<Integer> unpublish = requests.submit(() -> change("unpublish"));
      awaitRequestsWaitingForALock(2);

      offeringLock.commit();
      // with the purchase's lock, the unpublish waits for the purchase; without it, the unpublish
      // is through
      await(
          "the unpublish done or waiting for the purchase",
          () -> unpublish.isDone() || aRequestWaitsForTheOther(testBackends));
      foreignKeyLock.commit();

      purchased = purchase.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
      unpublished = unpublish.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    } finally {
      shutDown(requests);
    }

    // purchase first: the unpublish finds the case; unpublish first: the purchase finds a draft
    assertThat(List.of(purchased, unpublished))
        .as("purchase and unpublish")
        .isIn(List.of(201, 409), List.of(409, 200));
    assertThat(status() + " with " + cases() + " case(s)")
        .isEqualTo(purchased == 201 ? "PUBLISHED with 1 case(s)" : "DRAFT with 0 case(s)");
  }

  private int change(String action) {
    return mockMvc
        .post()
        .uri("/api/admin/consultation-offerings/{id}/" + action, AGILE_COACHING)
        .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
        .with(Csrf.token())
        .exchange()
        .getResponse()
        .getStatus();
  }

  private int purchase() {
    return mockMvc
        .post()
        .uri("/api/customer/consultation-cases")
        .with(LoggedIn.as(SeedData.CUSTOMER_1, Role.CUSTOMER))
        .with(Csrf.token())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            { "offeringId": "%s", "packageId": "%s", "price": "250.00", "sessionCount": 1 }
            """
                .formatted(AGILE_COACHING, AGILE_COACHING_1_SESSION))
        .exchange()
        .getResponse()
        .getStatus();
  }

  /**
   * Opens a transaction on the connection and locks Agile Coaching's row with the statement, until
   * it commits.
   */
  private static void lock(Connection connection, String statement) throws SQLException {
    connection.setAutoCommit(false);
    try (PreparedStatement lock = connection.prepareStatement(statement)) {
      lock.setObject(1, AGILE_COACHING);
      lock.executeQuery().close();
    }
  }

  /**
   * The connection's Postgres backend, as {@code pg_stat_activity} and {@code pg_blocking_pids}
   * name it.
   */
  private static int backend(Connection connection) throws SQLException {
    try (PreparedStatement query = connection.prepareStatement("select pg_backend_pid()");
        ResultSet result = query.executeQuery()) {
      result.next();
      return result.getInt(1);
    }
  }

  private String status() {
    return jdbc.sql("select status from offering where id = :id")
        .param("id", AGILE_COACHING)
        .query(String.class)
        .single();
  }

  private long cases() {
    return jdbc.sql("select count(*) from consultation_case where consultation_offering_id = :id")
        .param("id", AGILE_COACHING)
        .query(Long.class)
        .single();
  }

  private static void shutDown(ExecutorService requests) throws InterruptedException {
    requests.shutdownNow();
    // A failed run waits for its requests, so they can't race the cleanup script.
    requests.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
  }

  private void awaitRequestsWaitingForALock(int count) throws InterruptedException {
    await(count + " requests waiting for a lock", () -> requestsWaitingForALock() >= count);
  }

  private static void await(String description, BooleanSupplier condition)
      throws InterruptedException {
    Instant deadline = Instant.now().plus(TIMEOUT);
    while (!condition.getAsBoolean()) {
      assertThat(Instant.now()).as(description).isBefore(deadline);
      Thread.sleep(10);
    }
  }

  /**
   * The requests' statements waiting for a lock: those on the {@code offering} or {@code
   * consultation_case} table, a word in their text.
   */
  private long requestsWaitingForALock() {
    return jdbc.sql(
            """
            select count(*) from pg_stat_activity
            where datname = current_database() and wait_event_type = 'Lock'
              and query ~ '\\m(offering|consultation_case)\\M'
            """)
        .query(Long.class)
        .single();
  }

  /**
   * Whether a request waits for a lock that neither of the test's transactions holds: one of the
   * other request.
   */
  private boolean aRequestWaitsForTheOther(List<Integer> testBackends) {
    return jdbc.sql(
            """
            select exists (
                select 1 from pg_stat_activity a, unnest(pg_blocking_pids(a.pid)) as blocking(pid)
                where a.datname = current_database() and blocking.pid not in (:testBackends))
            """)
        .param("testBackends", testBackends)
        .query(Boolean.class)
        .single();
  }
}
