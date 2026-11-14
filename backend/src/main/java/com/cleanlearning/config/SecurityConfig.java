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
 * Who may call a route is checked twice (D-26). Each controller says it with {@code @PreAuthorize},
 * which is checked only when the controller method is called, after Spring MVC has read and
 * validated the request; so the filter chain below checks the role per prefix first, and a caller
 * without it is turned away before any body is read. A denial by either is answered by the filter
 * chain: 401 for a visitor, 403 for a user without the role.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {

  private static final String SESSION_COOKIE = "JSESSIONID";

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      SecurityContextRepository securityContextRepository,
      CsrfTokenRepository csrfTokenRepository) {
    http
        // A coarse first line, the same role as the controllers' @PreAuthorize under each prefix: a
        // caller without it gets no body read, parsed or validated, not even an image upload.
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
        // The token in a cookie JavaScript can read, and back in a header. Spring writes the cookie
        // on every response to a request without one, so the SPA's first GET gets it.
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
        // An anonymous caller gets a plain 401: no redirect, no login page, no Basic challenge, and
        // no session that saves the request for a redirect after login.
        .exceptionHandling(exceptions ->
            exceptions.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .requestCache(cache -> cache.requestCache(new NullRequestCache()));
    return http.build();
  }

  /** A bean so the PoC login saves to the one the filter chain reads. */
  @Bean
  SecurityContextRepository securityContextRepository() {
    return new DelegatingSecurityContextRepository(
        new HttpSessionSecurityContextRepository(),
        new RequestAttributeSecurityContextRepository());
  }

  /** A bean so a login replaces the token in the same place. */
  @Bean
  CsrfTokenRepository csrfTokenRepository() {
    return CookieCsrfTokenRepository.withHttpOnlyFalse();
  }

  /**
   * The PoC login calls it, because it isn't one of Spring Security's logins: a new session id
   * against session fixation, and a new CSRF token.
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
