package com.cleanlearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Users and login come from our own database, not a generated default user.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CleanLearningApplication {

  public static void main(String[] args) {
    SpringApplication.run(CleanLearningApplication.class, args);
  }
}
