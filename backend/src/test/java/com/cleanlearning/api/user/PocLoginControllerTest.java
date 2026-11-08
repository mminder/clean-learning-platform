package com.cleanlearning.api.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

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

import jakarta.servlet.http.Cookie;

/**
 * UC-01's PoC login: the user picker and {@code POST /api/poc/login}, and the CSRF token as the SPA handles it.
 */
@IntegrationTest
class PocLoginControllerTest {

    @Autowired
    MockMvcTester mockMvc;

    @Test
    void listsTheSeededUsersWithTheirRolesSortedByName() {
        assertThat(mockMvc.get().uri("/api/poc/users"))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          { "id": "00000000-0000-0000-0000-000000000301", "name": "Admin 1", "roles": ["ADMIN"] },
                          { "id": "00000000-0000-0000-0000-000000000302", "name": "Admin 2", "roles": ["ADMIN"] },
                          { "id": "00000000-0000-0000-0000-000000000101", "name": "Customer 1", "roles": ["CUSTOMER"] },
                          { "id": "00000000-0000-0000-0000-000000000102", "name": "Customer 2", "roles": ["CUSTOMER"] },
                          { "id": "00000000-0000-0000-0000-000000000201", "name": "Expert 1", "roles": ["CUSTOMER", "EXPERT"] },
                          { "id": "00000000-0000-0000-0000-000000000202", "name": "Expert 2", "roles": ["EXPERT", "ADMIN"] }
                        ]
                        """);
    }

    @Test
    void logsInAndTheSessionKnowsTheUser() {
        MvcTestResult login = logIn(SeedData.ADMIN_1.toString());

        assertThat(login).hasStatusOk().bodyJson().isStrictlyEqualTo("""
                {
                  "id": "00000000-0000-0000-0000-000000000301",
                  "name": "Admin 1",
                  "email": "admin1@example.com",
                  "roles": ["ADMIN"]
                }
                """);

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(mockMvc.get().uri("/api/me").session(session))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Admin 1");
    }

    /**
     * The SPA's way in a fresh browser: its first GET gets the CSRF cookie, the login sends the token back in the
     * header and gets a new session id and a new token, and the logout sends the new token.
     */
    @Test
    void firstLoginInAFreshBrowserAndLogout() {
        MvcTestResult visit = mockMvc.get().uri("/api/me").exchange();
        assertThat(visit).hasStatus(HttpStatus.UNAUTHORIZED);
        Cookie visitToken = lastCookie(visit, Csrf.COOKIE);
        assertThat(visitToken.getValue()).isNotEmpty();
        assertThat(visitToken.isHttpOnly()).as("readable by JavaScript").isFalse();
        assertThat(visitToken.getPath()).isEqualTo("/");

        // A session from before the login, as an attacker could plant it (session fixation).
        MockHttpSession session = new MockHttpSession();
        String sessionIdBeforeLogin = session.getId();
        MvcTestResult login = mockMvc.post().uri("/api/poc/login")
                .session(session)
                .with(request -> {
                    // the session's cookie, as the browser sends it
                    request.setRequestedSessionId(sessionIdBeforeLogin);
                    return request;
                })
                .cookie(visitToken)
                .header(Csrf.HEADER, visitToken.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "userId": "%s" }
                        """.formatted(SeedData.CUSTOMER_1))
                .exchange();

        assertThat(login).hasStatusOk();
        assertThat(session.getId()).as("session id").isNotEqualTo(sessionIdBeforeLogin);
        Cookie loginToken = lastCookie(login, Csrf.COOKIE);
        assertThat(loginToken.getValue()).isNotEmpty().isNotEqualTo(visitToken.getValue());
        assertThat(mockMvc.get().uri("/api/me").session(session))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Customer 1");

        MvcTestResult logout = mockMvc.post().uri("/api/logout")
                .session(session)
                .cookie(loginToken)
                .header(Csrf.HEADER, loginToken.getValue())
                .exchange();

        assertThat(logout).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mockMvc.get().uri("/api/me").session(session)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void answers403WithoutACsrfToken() {
        assertThat(mockMvc.post().uri("/api/poc/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "userId": "%s" }
                        """.formatted(SeedData.ADMIN_1)))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void answers404AsProblemDetailForAnUnknownUser() {
        MvcTestResult login = logIn("00000000-0000-0000-0000-000000000999");

        assertThat(login)
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "User 00000000-0000-0000-0000-000000000999 not found.",
                          "instance": "/api/poc/login"
                        }
                        """);
        assertThat(login.getRequest().getSession(false)).as("session").isNull();
    }

    @Test
    void answers400WithoutAUserId() {
        assertThat(mockMvc.post().uri("/api/poc/login")
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    private MvcTestResult logIn(String userId) {
        return mockMvc.post().uri("/api/poc/login")
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "userId": "%s" }
                        """.formatted(userId))
                .exchange();
    }

    /** The cookie the browser keeps: a response may delete a cookie and then set it again. */
    private static Cookie lastCookie(MvcTestResult result, String name) {
        return Arrays.stream(result.getResponse().getCookies())
                .filter(cookie -> cookie.getName().equals(name))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("no " + name + " cookie"));
    }
}
