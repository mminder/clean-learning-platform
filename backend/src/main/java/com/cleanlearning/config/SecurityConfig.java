package com.cleanlearning.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 * The session holds the login (D-25), and the SPA sends the CSRF token from the {@code XSRF-TOKEN}
 * cookie back in the {@code X-XSRF-TOKEN} header on every mutating request.
 *
 * <p>Who may call a route is checked twice (D-26, _docs/architecture/security.md). Each controller
 * says it with {@code @PreAuthorize}, which {@code @EnableMethodSecurity} turns on; {@code
 * AuthorizationRulesTest} fails the build for a handler method without one, and every route gets a
 * row in {@code RoleMatrixTest}. But the annotation is checked only when the controller method is
 * called, after Spring MVC has read and validated the request, so the filter chain below checks the
 * role per prefix first: a caller without it is turned away before any body is read. A denial by
 * either is answered by the filter chain: 401 for a visitor, 403 for a user without the role.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

  /** The servlet container's session cookie; {@code application.yml} sets its attributes. */
  private static final String SESSION_COOKIE = "JSESSIONID";

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      SecurityContextRepository securityContextRepository,
      CsrfTokenRepository csrfTokenRepository) {
    http
        // A coarse first line, the same role as the controllers' @PreAuthorize under each prefix: a
        // caller
        // without it gets no body read, parsed or validated, not even an image upload. Everything
        // else passes
        // on to the controllers' @PreAuthorize, or is public: the SPA's files, the OpenAPI document
        // (D-29) and
        // the logout below.
        .authorizeHttpRequests(requests ->
                requests
                    .requestMatchers("/api/customer/**")
                    .hasRole("CUSTOMER")
                    .requestMatchers("/api/expert/**")
                    .hasRole("EXPERT")
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .permitAll())
        .securityContext(context -> context.securityContextRepository(securityContextRepository))
        // Spring Security's SPA setup: the token in a cookie JavaScript can read, and back in a
        // header. It
        // writes the cookie on every response to a request without one, so the SPA's first GET gets
        // it.
        .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
        // A filter, not a controller: it answers itself, a 204 for a visitor, too.
        .logout(logout ->
            logout
                .logoutUrl("/api/logout")
                .logoutSuccessHandler(
                    new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
                .deleteCookies(SESSION_COOKIE))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        // A denial by a prefix above or by a @PreAuthorize reaches these handlers. An anonymous
        // caller gets a
        // plain 401: no redirect, no login page, no Basic challenge, and no session that saves the
        // request for
        // a redirect after login.
        .exceptionHandling(exceptions ->
            exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .requestCache(cache -> cache.requestCache(new NullRequestCache()));
    return http.build();
  }

  /**
   * Where the login is kept: in the session. Spring Security's default, as a bean so the PoC login
   * saves to the one the filter chain reads.
   */
  @Bean
  SecurityContextRepository securityContextRepository() {
    return new DelegatingSecurityContextRepository(
        new HttpSessionSecurityContextRepository(),
        new RequestAttributeSecurityContextRepository());
  }

  /**
   * The CSRF token in the {@code XSRF-TOKEN} cookie, the same as {@code csrf.spa()} sets, as a bean
   * so a login replaces the token in the same place.
   */
  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return CookieCsrfTokenRepository.withHttpOnlyFalse();
  }

  /**
   * What a login does to the session, as Spring Security's own logins do: a new session id against
   * session fixation, and a new CSRF token. The PoC login calls it, because it isn't one of Spring
   * Security's logins.
   */
  @Bean
  SessionAuthenticationStrategy sessionAuthenticationStrategy(
      CsrfTokenRepository csrfTokenRepository) {
    CsrfAuthenticationStrategy newCsrfToken = new CsrfAuthenticationStrategy(csrfTokenRepository);
    // Like csrf.spa(): create the new token at once, so the login's response already sets its
    // cookie.
    XorCsrfTokenRequestAttributeHandler requestHandler = new XorCsrfTokenRequestAttributeHandler();
    requestHandler.setCsrfRequestAttributeName(null);
    newCsrfToken.setRequestHandler(requestHandler);
    return new CompositeSessionAuthenticationStrategy(
        List.of(new ChangeSessionIdAuthenticationStrategy(), newCsrfToken));
  }
}
