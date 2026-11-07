package com.cleanlearning;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class CleanLearningApplicationTest {

    @Autowired
    MockMvcTester mockMvc;

    @Test
    void contextStartsAgainstPostgres() {
        assertThat(mockMvc).isNotNull();
    }
}
