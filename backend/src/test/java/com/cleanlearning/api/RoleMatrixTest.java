package com.cleanlearning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.domain.user.value.Role;

/**
 * Every route, called by every kind of user: the route either passes security, or the caller gets 401 (anonymous)
 * or 403 (missing role). See _docs/architecture/security.md.
 */
@IntegrationTest
class RoleMatrixTest {

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    // Every new route gets a row here. Until a prefix has a real route, a probe path stands in for it: security
    // decides before routing, so a probe that passes security ends in 404. An allowed caller must not change data, so
    // a mutating row sends no body (400, or 415 for a multipart route) or targets an unknown id (404).
    private static final List<Route> ROUTES = List.of(
            new Route(GET, "/api/shop/offerings", Access.PUBLIC),
            new Route(GET, "/api/shop/offerings/" + SeedData.CAREER_COACHING, Access.PUBLIC),
            new Route(GET, "/api/poc/users", Access.PUBLIC),
            new Route(POST, "/api/poc/login", Access.PUBLIC), // no body: 400
            new Route(GET, "/api/me", Access.AUTHENTICATED),
            // Spring Security's logout filter answers before authorization: a visitor's logout is a harmless 204.
            new Route(POST, "/api/logout", Access.PUBLIC),
            new Route(GET, "/api/probe", Access.AUTHENTICATED), // any /api route without a rule of its own
            new Route(GET, "/api/expert/probe", Access.EXPERT),
            new Route(GET, "/api/admin/probe", Access.ADMIN),
            new Route(POST, "/api/customer/consultation-cases", Access.CUSTOMER), // no body: 400
            new Route(GET, "/api/customer/consultation-cases", Access.CUSTOMER),
            new Route(GET, "/api/customer/consultation-cases/" + UNKNOWN_ID, Access.CUSTOMER), // unknown id: 404
            new Route(GET, "/api/admin/consultation-offerings", Access.ADMIN),
            new Route(POST, "/api/admin/consultation-offerings", Access.ADMIN), // no body: 400
            new Route(GET, "/api/admin/consultation-offerings/" + SeedData.CAREER_COACHING, Access.ADMIN),
            new Route(PUT, "/api/admin/consultation-offerings/" + UNKNOWN_ID, Access.ADMIN), // no body: 400
            // unknown id: 404
            new Route(POST, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/publish", Access.ADMIN),
            // unknown id: 404
            new Route(POST, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/unpublish", Access.ADMIN),
            // unknown id: 404
            new Route(POST, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/archive", Access.ADMIN),
            new Route(DELETE, "/api/admin/consultation-offerings/" + UNKNOWN_ID, Access.ADMIN), // unknown id: 404
            new Route(PUT, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/image", Access.ADMIN), // no body: 415
            // unknown id: 404
            new Route(DELETE, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/image", Access.ADMIN),
            new Route(GET, "/api/admin/experts", Access.ADMIN),
            new Route(GET, "/api/images/" + UNKNOWN_ID, Access.PUBLIC), // unknown id: 404
            new Route(GET, "/v3/api-docs", Access.PUBLIC),
            new Route(GET, "/", Access.PUBLIC),
            new Route(GET, "/offerings/anything", Access.PUBLIC));

    @Autowired
    MockMvcTester mockMvc;

    @ParameterizedTest(name = "{0} as {1}")
    @MethodSource("routesAndCallers")
    void securityDecidesByRole(Route route, Caller caller) {
        // CSRF is not what this test is about, so every request carries a valid token.
        MvcTestResult result = mockMvc.method(route.method()).uri(route.path())
                .with(caller.requestPostProcessor())
                .with(Csrf.token())
                .exchange();

        if (route.access().allows(caller)) {
            assertThat(result.getResponse().getStatus())
                    .as("passes security")
                    .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
        } else if (caller == Caller.ANONYMOUS) {
            assertThat(result)
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .doesNotContainHeader(HttpHeaders.LOCATION)
                    .doesNotContainHeader(HttpHeaders.WWW_AUTHENTICATE);
            assertThat(result.getRequest().getSession(false)).as("session").isNull();
        } else {
            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    static Stream<Arguments> routesAndCallers() {
        return ROUTES.stream()
                .flatMap(route -> Arrays.stream(Caller.values()).map(caller -> Arguments.of(route, caller)));
    }

    record Route(HttpMethod method, String path, Access access) {

        @Override
        public String toString() {
            return method + " " + path + " (" + access + ")";
        }
    }

    enum Access {
        PUBLIC, AUTHENTICATED, CUSTOMER, EXPERT, ADMIN;

        boolean allows(Caller caller) {
            return switch (this) {
                case PUBLIC -> true;
                case AUTHENTICATED -> caller != Caller.ANONYMOUS;
                case CUSTOMER, EXPERT, ADMIN -> caller.roles.contains(Role.valueOf(name()));
            };
        }
    }

    enum Caller {
        ANONYMOUS(null),
        CUSTOMER(SeedData.CUSTOMER_1, Role.CUSTOMER),
        // Expert 1 is a customer, too; here they call with EXPERT alone.
        EXPERT(SeedData.EXPERT_1, Role.EXPERT),
        ADMIN(SeedData.ADMIN_1, Role.ADMIN),
        EXPERT_AND_ADMIN(SeedData.EXPERT_2, Role.EXPERT, Role.ADMIN);

        private final UUID userId;

        private final List<Role> roles;

        Caller(UUID userId, Role... roles) {
            this.userId = userId;
            this.roles = List.of(roles);
        }

        RequestPostProcessor requestPostProcessor() {
            if (userId == null) {
                return anonymous();
            }
            return LoggedIn.as(userId, roles.toArray(Role[]::new));
        }
    }
}
