package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class CleanLearningApplicationTest {

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    ApplicationContext context;

    @Test
    void contextStartsAgainstPostgres() {
        assertThat(mockMvc).isNotNull();
    }

    @Test
    void createsNoDefaultUser() {
        // Spring Boot's default user comes with a generated password in the log.
        assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
    }
}
