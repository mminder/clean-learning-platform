package com.cleanlearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// No default user with a generated password: users and login come from our own database (UC-01).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CleanLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(CleanLearningApplication.class, args);
    }
}
