package com.cleanlearning;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * An integration test: the full application with MockMvc, the {@code poc} profile and a
 * Testcontainers Postgres. Every integration test uses exactly this setup, so the whole run shares
 * one Spring context and one container.
 *
 * <p>The application also listens on a random port ({@code @LocalServerPort}), for the few tests
 * that need the real servlet container: MockMvc skips it, and with it the container's multipart
 * limit ({@code ImageUploadLimitTest}).
 *
 * <p>The application's clock is a {@link TestClock}: the system clock until a test sets it, and
 * again after each test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("poc")
@Import({TestcontainersConfiguration.class, TestClock.Configuration.class})
@ExtendWith(TestClock.Reset.class)
public @interface IntegrationTest {}
