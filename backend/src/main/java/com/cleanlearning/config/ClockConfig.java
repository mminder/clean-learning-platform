package com.cleanlearning.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application's {@link Clock}: every use case gets the current time from it, never from {@code Instant.now()}.
 * Instants don't depend on the zone; a rule that needs a local date gets the business time zone, Europe/Zurich.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Zurich"));
    }
}
