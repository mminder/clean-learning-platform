package com.cleanlearning.api.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.cleanlearning.Csrf;
import com.cleanlearning.IntegrationTest;
import com.cleanlearning.LoggedIn;
import com.cleanlearning.SeedData;
import com.cleanlearning.TestImages;
import com.cleanlearning.domain.user.value.Role;
import com.jayway.jsonpath.JsonPath;

/**
 * {@code GET /api/images/{id}} serves an image to everyone and lets browsers cache it for good (D-21). The image is an
 * offering's, uploaded through the admin's route.
 */
@IntegrationTest
class ImageControllerTest {

    private static final String UNKNOWN_ID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    MockMvcTester mockMvc;

    /** A new draft with this PNG as its image; returns the image's id. */
    private String uploadedImage(byte[] png) throws Exception {
        MvcTestResult created = mockMvc.post().uri("/api/admin/consultation-offerings")
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "name": "Agile Coaching", "description": "", "sessionDurationMinutes": 45,
                          "cancellationDeadlineHours": 48, "packages": [], "eligibleExpertIds": [] }
                        """)
                .exchange();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        MvcTestResult uploaded = mockMvc.put().uri("/api/admin/consultation-offerings/{id}/image", id)
                .with(LoggedIn.as(SeedData.ADMIN_1, Role.ADMIN))
                .with(Csrf.token())
                .multipart()
                .file(new MockMultipartFile("file", "agile.png", "image/png", png))
                .exchange();
        return JsonPath.read(uploaded.getResponse().getContentAsString(), "$.imageId");
    }

    @Test
    @Sql(scripts = SeedData.DELETE_TEST_OFFERINGS, executionPhase = AFTER_TEST_METHOD)
    void servesAnImageToAVisitorWithItsTypeAndCachesItForGood() throws Exception {
        byte[] png = TestImages.png(100);
        String imageId = uploadedImage(png);

        MvcTestResult result = mockMvc.get().uri("/api/images/{id}", imageId).exchange();

        assertThat(result)
                .hasStatusOk()
                .hasContentType("image/png")
                .hasHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                // the browser keeps to the content type and doesn't guess another one from the bytes
                .hasHeader("X-Content-Type-Options", "nosniff")
                // Spring Security's no-cache headers stay off
                .doesNotContainHeader(HttpHeaders.PRAGMA)
                .doesNotContainHeader(HttpHeaders.EXPIRES);
        assertThat(result.getResponse().getHeaders(HttpHeaders.CACHE_CONTROL)).hasSize(1);
        assertThat(result).body().isEqualTo(png);
    }

    @Test
    void answersAnUnknownImageWithNotFound() {
        assertThat(mockMvc.get().uri("/api/images/{id}", UNKNOWN_ID))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Image %s not found.",
                          "instance": "/api/images/%s"
                        }
                        """.formatted(UNKNOWN_ID, UNKNOWN_ID));
    }

    @Test
    void answersAMalformedIdWithBadRequest() {
        assertThat(mockMvc.get().uri("/api/images/not-a-uuid")).hasStatus(HttpStatus.BAD_REQUEST);
    }
}
