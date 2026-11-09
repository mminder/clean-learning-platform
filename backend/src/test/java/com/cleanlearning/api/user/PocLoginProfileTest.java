package com.cleanlearning.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.cleanlearning.Csrf;
import com.cleanlearning.SeedData;
import com.cleanlearning.application.user.ListPocUsersUseCase;
import com.cleanlearning.application.user.LogInPocUserUseCase;
import com.cleanlearning.config.SecurityConfig;

/**
 * Without the {@code poc} profile, the PoC login doesn't exist (D-25): its routes pass security and answer 404. A web
 * slice without a database, so it needs no second Postgres container. The use cases are mocks, so the controller would
 * answer if its profile guard were missing.
 */
@WebMvcTest(PocLoginController.class)
@Import(SecurityConfig.class)
class PocLoginProfileTest {

    @MockitoBean
    ListPocUsersUseCase listPocUsers;

    @MockitoBean
    LogInPocUserUseCase logInPocUser;

    @Autowired
    MockMvcTester mockMvc;

    @Test
    void usersAreNotFound() {
        assertThat(mockMvc.get().uri("/api/poc/users")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void loginIsNotFound() {
        assertThat(mockMvc.post().uri("/api/poc/login")
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "userId": "%s" }
                        """.formatted(SeedData.ADMIN_1)))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
