package com.cleanlearning;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.cleanlearning.domain.shared.BusinessTimeZone;

/**
 * The application's {@link Clock} in integration tests: the system clock until a test sets an instant with
 * {@link #set(Instant)}, for example Mon 9 Nov 2026, 15:00 in Zurich. {@link IntegrationTest} registers it as the
 * primary clock ({@link Configuration}) and resets it to the system clock after each test ({@link Reset}), so a test
 * that forgets doesn't move the other tests' time. The clock's zone is Zurich, like the application's, unless a test
 * sets another with {@link #set(Instant, ZoneId)}, to show that the code takes the date in Zurich, not in the clock's
 * zone. Domain tests pass {@code now} in instead.
 */
public final class TestClock extends Clock {

    // null: the system clock. Requests may run on other threads than the test's.
    private volatile Instant fixed;

    private volatile ZoneId zone = BusinessTimeZone.ZONE;

    /** From now on, the clock answers this instant, until the test sets another or ends. */
    public void set(Instant instant) {
        fixed = instant;
    }

    /** Like {@link #set(Instant)}, and the clock's own zone is {@code zone}, not Zurich, until the test ends. */
    public void set(Instant instant, ZoneId zone) {
        fixed = instant;
        this.zone = zone;
    }

    /** The system clock again, in Zurich. */
    public void reset() {
        fixed = null;
        zone = BusinessTimeZone.ZONE;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        Instant instant = fixed;
        return instant == null ? Clock.system(zone) : Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        Instant instant = fixed;
        return instant == null ? Instant.now() : instant;
    }

    /** Registers the test clock as the primary {@link Clock}, ahead of {@code ClockConfig}'s. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class Configuration {

        @Bean
        @Primary
        TestClock testClock() {
            return new TestClock();
        }
    }

    /** Resets the test clock of the test's Spring context after each test. */
    public static class Reset implements AfterEachCallback {

        @Override
        public void afterEach(ExtensionContext context) {
            SpringExtension.getApplicationContext(context).getBean(TestClock.class).reset();
        }
    }
}
