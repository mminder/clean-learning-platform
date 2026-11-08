package com.cleanlearning.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Roles by route prefix, see _docs/architecture/security.md. A new {@code /api} route needs a logged-in user unless a
 * rule here says otherwise; every route gets a row in {@code RoleMatrixTest}.
 * <p>
 * The session holds the login (D-25), and the SPA sends the CSRF token from the {@code XSRF-TOKEN} cookie back in the
 * {@code X-XSRF-TOKEN} header on every mutating request.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /** The servlet container's session cookie; {@code application.yml} sets its attributes. */
    private static final String SESSION_COOKIE = "JSESSIONID";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository) {
        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/shop/**").permitAll()
                        // The PoC login. Its controller exists only in the poc profile, elsewhere the routes are 404.
                        .requestMatchers("/api/poc/**").permitAll()
                        // the OpenAPI document (D-29)
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/expert/**").hasRole("EXPERT")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").authenticated()
                        // the SPA's files
                        .anyRequest().permitAll())
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                // Spring Security's SPA setup: the token in a cookie JavaScript can read, and back in a header. It
                // writes the cookie on every response to a request without one, so the SPA's first GET gets it.
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
                // Answers before authorization, so it's a 204 for a visitor, too.
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                        .deleteCookies(SESSION_COOKIE))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // An anonymous caller gets a plain 401: no redirect, no login page, no Basic challenge,
                // and no session that saves the request for a redirect after login.
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()));
        return http.build();
    }

    /**
     * Where the login is kept: in the session. Spring Security's default, as a bean so the PoC login saves to the one
     * the filter chain reads.
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(new HttpSessionSecurityContextRepository(),
                new RequestAttributeSecurityContextRepository());
    }

    /**
     * The CSRF token in the {@code XSRF-TOKEN} cookie, the same as {@code csrf.spa()} sets, as a bean so a login
     * replaces the token in the same place.
     */
    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /**
     * What a login does to the session, as Spring Security's own logins do: a new session id against session
     * fixation, and a new CSRF token. The PoC login calls it, because it isn't one of Spring Security's logins.
     */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
        CsrfAuthenticationStrategy newCsrfToken = new CsrfAuthenticationStrategy(csrfTokenRepository);
        // Like csrf.spa(): create the new token at once, so the login's response already sets its cookie.
        XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);
        newCsrfToken.setRequestHandler(requestHandler);
        return new CompositeSessionAuthenticationStrategy(
                List.of(new ChangeSessionIdAuthenticationStrategy(), newCsrfToken));
    }
}
