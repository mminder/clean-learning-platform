package com.cleanlearning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.Arrays;
import java.util.List;
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

import com.cleanlearning.IntegrationTest;

/**
 * Every route, called by every kind of user: the route either passes security, or the caller gets 401 (anonymous)
 * or 403 (missing role). See _docs/architecture/security.md.
 */
@IntegrationTest
class RoleMatrixTest {

    // Every new route gets a row here. Until a prefix has a real route, a probe path stands in for it: security
    // decides before routing, so a probe that passes security ends in 404.
    private static final List<Route> ROUTES = List.of(
            new Route(GET, "/api/shop/probe", Access.PUBLIC),
            new Route(GET, "/api/me", Access.AUTHENTICATED),
            new Route(GET, "/api/customer/probe", Access.CUSTOMER),
            new Route(GET, "/api/expert/probe", Access.EXPERT),
            new Route(GET, "/api/admin/probe", Access.ADMIN),
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
                .with(csrf())
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
                case CUSTOMER, EXPERT, ADMIN -> caller.roles.contains(name());
            };
        }
    }

    enum Caller {
        ANONYMOUS(),
        CUSTOMER("CUSTOMER"),
        EXPERT("EXPERT"),
        ADMIN("ADMIN"),
        EXPERT_AND_ADMIN("EXPERT", "ADMIN");

        private final List<String> roles;

        Caller(String... roles) {
            this.roles = List.of(roles);
        }

        RequestPostProcessor requestPostProcessor() {
            if (roles.isEmpty()) {
                return anonymous();
            }
            return user(name().toLowerCase()).roles(roles.toArray(String[]::new));
        }
    }
}
