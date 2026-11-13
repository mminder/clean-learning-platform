package com.cleanlearning;

import jakarta.servlet.http.Cookie;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * A request with a CSRF token, sent the way the SPA sends it: the {@code XSRF-TOKEN} cookie, and
 * the same value in the {@code X-XSRF-TOKEN} header.
 *
 * <p>Use this instead of Spring Security Test's {@code csrf()}: that one swaps the CSRF filter's
 * token repository for a session-based one, in the Spring context all integration tests share, so
 * every later test would run with another CSRF setup than the application's.
 */
public final class Csrf {

  public static final String COOKIE = "XSRF-TOKEN";

  public static final String HEADER = "X-XSRF-TOKEN";

  private Csrf() {}

  public static RequestPostProcessor token() {
    String token = UUID.randomUUID().toString();
    return request -> {
      request.setCookies(appended(request.getCookies(), new Cookie(COOKIE, token)));
      request.addHeader(HEADER, token);
      return request;
    };
  }

  private static Cookie[] appended(Cookie[] cookies, Cookie cookie) {
    if (cookies == null) {
      return new Cookie[] {cookie};
    }
    Cookie[] result = Arrays.copyOf(cookies, cookies.length + 1);
    result[cookies.length] = cookie;
    return result;
  }
}
