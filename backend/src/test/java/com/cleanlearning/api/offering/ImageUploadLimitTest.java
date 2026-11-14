package com.cleanlearning.api.offering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestImages;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The upload limit of the real servlet container (D-21): Spring's default of 1 MB would refuse an
 * image of 2 MB, and a larger file must answer 400, not 413 or 500. MockMvc builds the multipart
 * request itself and never meets the limit, so this test sends the requests over HTTP, logged in
 * through the PoC login like the SPA, with its CSRF cookie and header.
 */
@IntegrationTest
class ImageUploadLimitTest {

  private static final String BOUNDARY = "image-upload-limit-test";

  @LocalServerPort int port;

  @Autowired MockMvcTester mockMvc;

  private final CookieManager cookies = new CookieManager();

  private final HttpClient http = HttpClient.newBuilder().cookieHandler(cookies).build();

  private String offeringId;

  @BeforeEach
  void logInAsAdminWithADraft() throws Exception {
    // The first request gets the CSRF cookie, as the SPA's first GET /api/me does.
    send(HttpRequest.newBuilder(uri("/api/me")).GET());
    HttpResponse<String> login =
        send(
            HttpRequest.newBuilder(uri("/api/poc/login"))
                .header("Content-Type", "application/json")
                .header(Csrf.HEADER, csrfToken())
                .POST(
                    BodyPublishers.ofString(
                        """
                        { "userId": "%s" }
                        """
                            .formatted(SeedData.ADMIN_1))));
    assertThat(login.statusCode()).isEqualTo(200);

    String created =
        mockMvc
            .post()
            .uri("/api/admin/consultation-offerings")
            .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
            .with(Csrf.token())
            .contentType("application/json")
            .content(
                """
                { "name": "Agile Coaching", "description": "", "sessionDurationMinutes": 45,
                  "cancellationDeadlineHours": 48, "packages": [], "eligibleExpertIds": [] }
                """)
            .exchange()
            .getResponse()
            .getContentAsString();
    offeringId = JsonPath.read(created, "$.id");
  }

  @Test
  @Sql(scripts = SeedData.DELETE_TEST_DATA, executionPhase = AFTER_TEST_METHOD)
  void acceptsTwoMegabytesAndAnswersOneByteMoreWithBadRequest() throws Exception {
    HttpResponse<String> accepted = upload(TestImages.jpeg(TestImages.MAX_BYTES));
    assertThat(accepted.statusCode()).isEqualTo(200);
    String imageId = JsonPath.read(accepted.body(), "$.imageId");

    HttpResponse<String> refused = upload(TestImages.jpeg(TestImages.MAX_BYTES + 1));

    assertThat(refused.statusCode()).isEqualTo(400);
    assertThat(refused.headers().firstValue("Content-Type")).hasValue("application/problem+json");
    assertThat((String) JsonPath.read(refused.body(), "$.detail"))
        .isEqualTo("An image is at most 2 MB.");
    // the offering keeps its image
    HttpResponse<String> offering =
        send(HttpRequest.newBuilder(uri("/api/admin/consultation-offerings/" + offeringId)).GET());
    assertThat((String) JsonPath.read(offering.body(), "$.imageId")).isEqualTo(imageId);
  }

  private HttpResponse<String> upload(byte[] jpeg) throws IOException, InterruptedException {
    return send(
        HttpRequest.newBuilder(uri("/api/admin/consultation-offerings/" + offeringId + "/image"))
            .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
            .header(Csrf.HEADER, csrfToken())
            .PUT(BodyPublishers.ofByteArray(multipart(jpeg))));
  }

  /** A multipart body with one part, {@code file}, as a browser sends a {@code FormData}. */
  private static byte[] multipart(byte[] jpeg) throws IOException {
    ByteArrayOutputStream body = new ByteArrayOutputStream();
    body.write(
        ("--"
                + BOUNDARY
                + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII));
    body.write(jpeg);
    body.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.US_ASCII));
    return body.toByteArray();
  }

  private String csrfToken() {
    return cookies.getCookieStore().getCookies().stream()
        .filter(cookie -> cookie.getName().equals(Csrf.COOKIE))
        .map(HttpCookie::getValue)
        .findFirst()
        .orElseThrow();
  }

  private HttpResponse<String> send(HttpRequest.Builder request)
      throws IOException, InterruptedException {
    return http.send(request.build(), BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
