package com.cleanlearning.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.SeedData;

/**
 * UC-01's log out: Spring Security's logout at {@code POST /api/logout}.
 */
@IntegrationTest
class LogoutTest {

    @Autowired
    MockMvcTester mockMvc;

    @Test
    void endsTheSessionAndDeletesItsCookie() {
        MockHttpSession session = logIn();

        MvcTestResult logout = mockMvc.post().uri("/api/logout").session(session).with(Csrf.token()).exchange();

        assertThat(logout).hasStatus(HttpStatus.NO_CONTENT).body().isEmpty();
        assertThat(logout).cookies().hasCookieSatisfying("JSESSIONID",
                cookie -> assertThat(cookie.getMaxAge()).as("max age").isZero());
        assertThat(session.isInvalid()).as("session invalidated").isTrue();
        assertThat(mockMvc.get().uri("/api/me").session(session)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void isA204ForAVisitorToo() {
        // The logout filter answers before authorization.
        assertThat(mockMvc.post().uri("/api/logout").with(Csrf.token())).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void answers403WithoutACsrfToken() {
        MockHttpSession session = logIn();

        assertThat(mockMvc.post().uri("/api/logout").session(session)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mockMvc.get().uri("/api/me").session(session)).as("still logged in").hasStatusOk();
    }

    @Test
    void theOldLogoutPathIsNotSpecial() {
        MockHttpSession session = logIn();

        MvcTestResult result = mockMvc.post().uri("/logout").session(session).with(Csrf.token()).exchange();

        assertThat(result.getResponse().getStatus()).isNotEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(mockMvc.get().uri("/api/me").session(session)).as("still logged in").hasStatusOk();
    }

    private MockHttpSession logIn() {
        MvcTestResult login = mockMvc.post().uri("/api/poc/login")
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "userId": "%s" }
                        """.formatted(SeedData.CUSTOMER_1))
                .exchange();
        assertThat(login).hasStatusOk();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
