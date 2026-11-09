package com.cleanlearning.api.consultation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

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

import org.junit.jupiter.api.BeforeEach;
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
import com.cleanlearning.TestClock;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;

/**
 * The booking's two locks (D-27): two bookings with one expert at once can't both take the expert's slot, and two
 * bookings of one case at once can't both take its last credit. A booking locks the case
 * ({@code LockCaseForBookingPort}), then the expert's user row ({@code LockExpertPort}), so they run one after the
 * other.
 * <p>
 * As in {@code ConsultationOfferingConcurrencyTest}, a test makes its race certain rather than likely: it holds the
 * contended row lock itself while the requests start, waits until Postgres shows both waiting for a lock, and only
 * then lets go. With the lock, they wait at the lock and then run one after the other. Without it, they read at once,
 * and both pass. "Now" is Mon 9 Nov 2026, 15:00 in Zurich; in November, Zurich is UTC+1.
 */
@IntegrationTest
class ConsultationCaseConcurrencyTest {

    private static final String PATH = "/api/customer/consultation-cases";

    /** Mon 9 Nov 2026, 15:00 in Zurich. */
    private static final Instant NOW = Instant.parse("2026-11-09T14:00:00Z");

    /** Tue 17 Nov 2026, 10:00 and 11:00 in Zurich. */
    private static final String TEN = "2026-11-17T09:00:00Z";
    private static final String ELEVEN = "2026-11-17T10:00:00Z";

    // Mock Interview from one-session-offering.sql: published, 60 minutes, both experts eligible
    private static final UUID MOCK_INTERVIEW = UUID.fromString("00000000-0000-0000-0000-000000009007");

    // its only package: 1 session for CHF 150
    private static final UUID MOCK_INTERVIEW_1_SESSION = UUID.fromString("00000000-0000-0000-0000-000000009071");

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    TestClock clock;

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcClient jdbc;

    @BeforeEach
    void setNow() {
        clock.set(NOW);
    }

    /**
     * The expert's lock: two customers book Expert 2 at the same start, each on their own case, so only the expert's
     * lock can order them. Without it, both find the slot free, and both book it.
     * <p>
     * Besides Expert 2's {@code app_user} row, which the expert's lock takes, the test holds their
     * {@code expert_profile} row, which each first booking's foreign key check of the case's expert waits for: so a
     * booking that has found the slot free can't commit until the test lets go. With the expert's lock, both wait at
     * the {@code app_user} row; then the first waits at the foreign key and holds the lock, and the second waits for
     * it. Without the lock, both wait at the foreign key. Only then does the test let go of the foreign key.
     */
    @Test
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void twoCustomersCantBothBookOneSlotOfAnExpert() throws Exception {
        available(SeedData.EXPERT_2);
        String ofCustomer1 = bought(SeedData.CUSTOMER_1, SeedData.CAREER_COACHING,
                SeedData.CAREER_COACHING_3_SESSIONS, "450.00", 3);
        String ofCustomer2 = bought(SeedData.CUSTOMER_2, SeedData.CAREER_COACHING,
                SeedData.CAREER_COACHING_3_SESSIONS, "450.00", 3);

        ExecutorService requests = Executors.newFixedThreadPool(2);
        List<String> answers;
        try (Connection expertLock = dataSource.getConnection();
                Connection foreignKeyLock = dataSource.getConnection()) {
            lock(expertLock, "select id from app_user where id = ? for no key update", SeedData.EXPERT_2);
            lock(foreignKeyLock, "select user_id from expert_profile where user_id = ? for update",
                    SeedData.EXPERT_2);
            int foreignKeyBackend = backend(foreignKeyLock);
            List<Integer> testBackends = List.of(backend(expertLock), foreignKeyBackend);

            Future<String> first = requests.submit(() -> book(ofCustomer1, SeedData.EXPERT_2, TEN,
                    SeedData.CUSTOMER_1));
            Future<String> second = requests.submit(() -> book(ofCustomer2, SeedData.EXPERT_2, TEN,
                    SeedData.CUSTOMER_2));
            awaitRequestsWaitingForALock(2);

            expertLock.commit();
            // with the expert's lock, one waits at the foreign key and the other for it; without it, both at the key
            await("2 requests waiting for the expert profile or for each other",
                    () -> requestsWaitingForTheForeignKeyOrEachOther(foreignKeyBackend, testBackends) >= 2);
            foreignKeyLock.commit();

            answers = List.of(first.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS),
                    second.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
        } finally {
            shutDown(requests);
        }

        assertThat(answers).as("the two bookings").containsExactlyInAnyOrder("201", "409 SLOT_NOT_AVAILABLE");
        assertThat(jdbc.sql("""
                select count(*) from appointment a join consultation_case c on c.id = a.consultation_case_id
                where c.assigned_expert_id = :expert and a.starts_at = :start::timestamptz
                """).param("expert", SeedData.EXPERT_2).param("start", TEN).query(Long.class).single())
                .as("Expert 2's appointments at the start").isOne();
    }

    /**
     * The case's lock: Customer 1 books their case's one credit twice at once, as from two tabs, once with Expert 1 and
     * once with Expert 2, so each locks another expert and only the case's lock can order them. Without it, both find
     * the credit open, and both book.
     * <p>
     * The test holds the case's row. With the case's lock, both wait at it. Without it, both wait at their update of
     * the case's expert, which needs the same row lock.
     */
    @Test
    @Sql("/com/cleanlearning/api/consultation/one-session-offering.sql")
    @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
    void twoBookingsOfOneCaseCantBothTakeItsLastCredit() throws Exception {
        available(SeedData.EXPERT_1);
        available(SeedData.EXPERT_2);
        String caseId = bought(SeedData.CUSTOMER_1, MOCK_INTERVIEW, MOCK_INTERVIEW_1_SESSION, "150.00", 1);

        ExecutorService requests = Executors.newFixedThreadPool(2);
        List<String> answers;
        try (Connection caseLock = dataSource.getConnection()) {
            lock(caseLock, "select id from consultation_case where id = ? for no key update", UUID.fromString(caseId));

            Future<String> withExpert1 = requests.submit(() -> book(caseId, SeedData.EXPERT_1, TEN,
                    SeedData.CUSTOMER_1));
            Future<String> withExpert2 = requests.submit(() -> book(caseId, SeedData.EXPERT_2, ELEVEN,
                    SeedData.CUSTOMER_1));
            awaitRequestsWaitingForALock(2);
            caseLock.commit();

            answers = List.of(withExpert1.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS),
                    withExpert2.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS));
        } finally {
            shutDown(requests);
        }

        assertThat(answers).as("the two bookings").containsExactlyInAnyOrder("201", "409 NO_OPEN_CREDITS");
        assertThat(jdbc.sql("select count(*) from appointment where consultation_case_id = :id::uuid")
                .param("id", caseId).query(Long.class).single()).as("the case's appointments").isOne();
    }

    /** The expert saves Tue 17 Nov 2026, 09:00–12:00 in Zurich. */
    private void available(UUID expert) {
        assertThat(mockMvc.put().uri("/api/expert/availability")
                .with(LoggedIn.as(expert, Role.EXPERT))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "dates": ["2026-11-17"], "timeRanges": [{ "start": "09:00", "end": "12:00" }] }
                        """))
                .hasStatusOk();
    }

    /** The customer buys the package; returns the new case's id. */
    private String bought(UUID customer, UUID offeringId, UUID packageId, String price, int sessionCount)
            throws Exception {
        MvcTestResult bought = mockMvc.post().uri(PATH)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "offeringId": "%s", "packageId": "%s", "price": "%s", "sessionCount": %d }
                        """.formatted(offeringId, packageId, price, sessionCount))
                .exchange();
        assertThat(bought).hasStatus(HttpStatus.CREATED);
        return JsonPath.read(bought.getResponse().getContentAsString(), "$.id");
    }

    /** Books the start with the expert; answers the status, and for a 409 its code too, as in "409 NO_OPEN_CREDITS". */
    private String book(String caseId, UUID expert, String start, UUID customer) throws Exception {
        MvcTestResult booked = mockMvc.post().uri(PATH + "/{id}/appointments", caseId)
                .with(LoggedIn.as(customer, Role.CUSTOMER))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "expertId": "%s", "start": "%s" }
                        """.formatted(expert, start))
                .exchange();
        int status = booked.getResponse().getStatus();
        return status == HttpStatus.CONFLICT.value()
                ? status + " " + JsonPath.read(booked.getResponse().getContentAsString(), "$.code")
                : String.valueOf(status);
    }

    /** Opens a transaction on the connection and locks the row with the statement, until it commits. */
    private static void lock(Connection connection, String statement, UUID id) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement lock = connection.prepareStatement(statement)) {
            lock.setObject(1, id);
            lock.executeQuery().close();
        }
    }

    /** The connection's Postgres backend, as {@code pg_stat_activity} and {@code pg_blocking_pids} name it. */
    private static int backend(Connection connection) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement("select pg_backend_pid()");
                ResultSet result = query.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void shutDown(ExecutorService requests) throws InterruptedException {
        requests.shutdownNow();
        // A failed run waits for its requests, so they can't race the cleanup script.
        requests.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    private void awaitRequestsWaitingForALock(int count) throws InterruptedException {
        await(count + " requests waiting for a lock", () -> requestsWaitingForALock() >= count);
    }

    private static void await(String description, BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (!condition.getAsBoolean()) {
            assertThat(Instant.now()).as(description).isBefore(deadline);
            Thread.sleep(10);
        }
    }

    /**
     * The requests' statements waiting for a lock: the booking's lock of the expert's {@code app_user} row or of the
     * case, or its update of the case, a word in their text.
     */
    private long requestsWaitingForALock() {
        return jdbc.sql("""
                select count(*) from pg_stat_activity
                where datname = current_database() and wait_event_type = 'Lock'
                  and query ~ '\\m(app_user|consultation_case)\\M'
                """).query(Long.class).single();
    }

    /**
     * The requests waiting for a lock that the test's {@code expert_profile} transaction holds, or that neither of the
     * test's transactions holds: one of the other request.
     */
    private long requestsWaitingForTheForeignKeyOrEachOther(int foreignKeyLock, List<Integer> testBackends) {
        return jdbc.sql("""
                select count(*) from pg_stat_activity a
                where a.datname = current_database() and a.wait_event_type = 'Lock'
                  and exists (
                    select 1 from unnest(pg_blocking_pids(a.pid)) as blocking(pid)
                    where blocking.pid = :foreignKeyLock or blocking.pid not in (:testBackends))
                """).param("foreignKeyLock", foreignKeyLock).param("testBackends", testBackends)
                .query(Long.class).single();
    }
}
