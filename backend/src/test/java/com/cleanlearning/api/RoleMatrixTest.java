package com.cleanlearning.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestImages;
import com.cleanlearning.domain.user.value.Role;

/**
 * Every route, called by every kind of user: the route either passes security, or the caller gets 401 (anonymous)
 * or 403 (missing role). Each controller says who may call it with {@code @PreAuthorize}, and under a role prefix
 * {@code SecurityConfig} says it first; this test proves what they say. See _docs/architecture/security.md.
 */
@IntegrationTest
class RoleMatrixTest {

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    private static final List<Access> ROLES = List.of(Access.CUSTOMER, Access.EXPERT, Access.ADMIN);

    /** An offering's form, valid as far as Bean Validation goes. */
    private static final String OFFERING = """
            {"name": "%s", "description": "", "sessionDurationMinutes": 45, "cancellationDeadlineHours": 24,
             "packages": [], "eligibleExpertIds": []}
            """;

    // Every route gets a row here; everyRouteHasARow() fails for a route without one. @PreAuthorize runs after Spring
    // MVC has read and validated the request, so a row with a body sends one that passes, and an allowed caller
    // reaches the annotation, too. An allowed caller must not change data, so the use case still says no: an unknown
    // id (404), or a value the domain refuses (400).
    private static final List<Route> ROUTES = List.of(
            Route.of(GET, "/api/shop/offerings", Access.PUBLIC),
            Route.of(GET, "/api/shop/offerings/" + SeedData.CAREER_COACHING, Access.PUBLIC),
            Route.of(GET, "/api/poc/users", Access.PUBLIC),
            // unknown user: 404
            Route.withJson(POST, "/api/poc/login", """
                    {"userId": "%s"}""".formatted(UNKNOWN_ID), Access.PUBLIC),
            Route.of(GET, "/api/me", Access.AUTHENTICATED),
            // A path under a role prefix without a route: the filter chain turns a caller without the role away before
            // Spring MVC sees the request, so before any body is read; a caller with the role gets 404.
            Route.of(GET, "/api/customer/probe", Access.CUSTOMER),
            Route.of(GET, "/api/expert/probe", Access.EXPERT),
            Route.of(GET, "/api/admin/probe", Access.ADMIN),
            // Spring Security's logout filter answers before any controller: a visitor's logout is a harmless 204.
            Route.of(POST, "/api/logout", Access.PUBLIC),
            // unknown offering: 404
            Route.withJson(POST, "/api/customer/consultation-cases", """
                    {"offeringId": "%s", "packageId": "%s", "price": "100.00", "sessionCount": 1}"""
                    .formatted(UNKNOWN_ID, UNKNOWN_ID), Access.CUSTOMER),
            Route.of(GET, "/api/customer/consultation-cases", Access.CUSTOMER),
            Route.of(GET, "/api/customer/consultation-cases/" + UNKNOWN_ID, Access.CUSTOMER), // unknown id: 404
            // unknown id: 404
            Route.of(GET, "/api/customer/consultation-cases/" + UNKNOWN_ID + "/eligible-experts", Access.CUSTOMER),
            // unknown id: 404
            Route.of(GET, "/api/customer/consultation-cases/" + UNKNOWN_ID + "/bookable-slots?expertId="
                    + SeedData.EXPERT_2, Access.CUSTOMER),
            // unknown id: 404
            Route.withJson(POST, "/api/customer/consultation-cases/" + UNKNOWN_ID + "/appointments", """
                    {"expertId": "%s", "start": "2030-01-07T08:00:00Z"}""".formatted(SeedData.EXPERT_2),
                    Access.CUSTOMER),
            // unknown case: 404
            Route.withJson(POST, "/api/customer/consultation-cases/" + UNKNOWN_ID + "/appointments/" + UNKNOWN_ID
                    + "/cancel", """
                    {"creditRefunded": true}""", Access.CUSTOMER),
            // unknown case: 404
            Route.withJson(POST, "/api/customer/consultation-cases/" + UNKNOWN_ID + "/cancel", """
                    {"refundEligible": true}""", Access.CUSTOMER),
            Route.of(GET, "/api/admin/consultation-offerings", Access.ADMIN),
            // A name of only a no-break space: Bean Validation lets it through, the value object answers 400.
            Route.withJson(POST, "/api/admin/consultation-offerings", OFFERING.formatted("\u00A0"), Access.ADMIN),
            Route.of(GET, "/api/admin/consultation-offerings/" + SeedData.CAREER_COACHING, Access.ADMIN),
            // unknown id: 404
            Route.withJson(PUT, "/api/admin/consultation-offerings/" + UNKNOWN_ID, OFFERING.formatted("Probe"),
                    Access.ADMIN),
            // unknown id: 404
            Route.of(POST, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/publish", Access.ADMIN),
            // unknown id: 404
            Route.of(POST, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/unpublish", Access.ADMIN),
            // unknown id: 404
            Route.of(POST, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/archive", Access.ADMIN),
            Route.of(DELETE, "/api/admin/consultation-offerings/" + UNKNOWN_ID, Access.ADMIN), // unknown id: 404
            // unknown id: 404
            Route.withImage(PUT, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/image", Access.ADMIN),
            // unknown id: 404
            Route.of(DELETE, "/api/admin/consultation-offerings/" + UNKNOWN_ID + "/image", Access.ADMIN),
            Route.of(GET, "/api/admin/experts", Access.ADMIN),
            Route.of(GET, "/api/expert/availability", Access.EXPERT),
            // A date before today: Bean Validation lets it through, the domain answers 400 before anything is saved.
            Route.withJson(PUT, "/api/expert/availability", """
                    {"dates": ["2000-01-03"], "timeRanges": []}""", Access.EXPERT),
            Route.of(GET, "/api/images/" + UNKNOWN_ID, Access.PUBLIC), // unknown id: 404
            Route.of(HEAD, "/api/images/" + UNKNOWN_ID, Access.PUBLIC), // the GET route answers HEAD, too
            Route.of(GET, "/v3/api-docs", Access.PUBLIC),
            Route.of(GET, "/", Access.PUBLIC),
            Route.of(GET, "/offerings/anything", Access.PUBLIC));

    @Autowired
    MockMvcTester mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping handlerMapping;

    @ParameterizedTest(name = "{0} as {1}")
    @MethodSource("routesAndCallers")
    void securityDecidesByRole(Route route, Caller caller) {
        // CSRF is not what this test is about, so every request carries a valid token.
        MvcTestResult result = mockMvc.perform(route.request().get()
                .with(caller.requestPostProcessor())
                .with(Csrf.token()));

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

    /** Each route of our controllers, every HTTP method of it, has a row, so no route escapes the matrix. */
    @Test
    void everyRouteHasARow() {
        List<String> routesWithoutARow = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getPackageName().startsWith("com.cleanlearning"))
                .flatMap(entry -> Mapping.of(entry.getKey()))
                .filter(mapping -> ROUTES.stream().noneMatch(mapping::isCoveredBy))
                .map(Mapping::toString)
                .sorted()
                .toList();

        assertThat(routesWithoutARow).as("routes without a row in ROUTES").isEmpty();
    }

    /**
     * The prefix names the role (D-26): a row under {@code /api/customer}, {@code /api/expert} or {@code /api/admin}
     * needs that role, and a row that needs a role is under its prefix.
     */
    @Test
    void theRolePrefixNamesTheRole() {
        List<Route> mismatched = ROUTES.stream()
                .filter(route -> ROLES.stream().anyMatch(role ->
                        route.path().startsWith("/api/" + role.name().toLowerCase(Locale.ROOT) + "/")
                                != (route.access() == role)))
                .toList();

        assertThat(mismatched).as("rows whose role prefix and access differ").isEmpty();
    }

    static Stream<Arguments> routesAndCallers() {
        return ROUTES.stream()
                .flatMap(route -> Arrays.stream(Caller.values()).map(caller -> Arguments.of(route, caller)));
    }

    /** A row: a request, built afresh for each caller, and who may make it. */
    record Route(HttpMethod method, String path, Access access,
            Supplier<AbstractMockHttpServletRequestBuilder<?>> request) {

        static Route of(HttpMethod method, String path, Access access) {
            return new Route(method, path, access, () -> MockMvcRequestBuilders.request(method, path));
        }

        static Route withJson(HttpMethod method, String path, String json, Access access) {
            return new Route(method, path, access, () -> MockMvcRequestBuilders.request(method, path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));
        }

        static Route withImage(HttpMethod method, String path, Access access) {
            return new Route(method, path, access, () -> MockMvcRequestBuilders.multipart(method, path)
                    .file(new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, TestImages.png(100))));
        }

        @Override
        public String toString() {
            return method + " " + path + " (" + access + ")";
        }
    }

    /** A route of a controller: one HTTP method and one path pattern, such as {@code GET /api/images/{id}}. */
    record Mapping(@Nullable HttpMethod method, PathPattern pattern) {

        static Stream<Mapping> of(RequestMappingInfo info) {
            Set<PathPattern> patterns = Objects.requireNonNull(info.getPathPatternsCondition(), "path patterns")
                    .getPatterns();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) {
                // A route for every HTTP method: no row can cover it, so it fails and names itself.
                return patterns.stream().map(pattern -> new Mapping(null, pattern));
            }
            return patterns.stream()
                    .flatMap(pattern -> methods.stream().map(method -> new Mapping(method.asHttpMethod(), pattern)));
        }

        boolean isCoveredBy(Route route) {
            String path = route.path().split("\\?", 2)[0];
            return route.method().equals(method) && pattern.matches(PathContainer.parsePath(path));
        }

        @Override
        public String toString() {
            return (method == null ? "every method" : method) + " " + pattern;
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
