package com.cleanlearning.config;

import com.cleanlearning.domain.shared.BusinessTimeZone;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Every use case gets the current time from it, never from {@code Instant.now()}. A rule that needs
 * a local date takes it in {@link BusinessTimeZone#ZONE}, never in the clock's own zone.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.system(BusinessTimeZone.ZONE);
  }
}
