package com.cleanlearning.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.cleanlearning.IntegrationTest;

/**
 * The jar serves the SPA: a client route gets {@code index.html}, a missing file or API path gets 404. The test
 * resources hold a stub {@code static/index.html}; the real one is copied in by {@code -Pfrontend}.
 */
@IntegrationTest
class SpaFallbackTest {

    private static final String INDEX_HTML_MARKER = "test index.html";

    @Autowired
    MockMvcTester mockMvc;

    @Test
    void rootForwardsToIndexHtml() {
        // Boot's welcome page; MockMvc doesn't follow the forward, a real server serves the file below.
        assertThat(mockMvc.get().uri("/")).hasStatusOk().hasForwardedUrl("index.html");
        assertIndexHtml(mockMvc.get().uri("/index.html").exchange());
    }

    @Test
    void clientRouteServesIndexHtml() {
        assertIndexHtml(mockMvc.get().uri("/offerings/00000000-0000-0000-0000-000000001001").exchange());
    }

    @Test
    void existingStaticFileIsServedAsItIs() {
        MvcTestResult result = mockMvc.get().uri("/stub.js").exchange();

        assertThat(result).hasStatusOk().bodyText().contains("stub static file");
    }

    @Test
    void missingAssetIs404() {
        assertThat(mockMvc.get().uri("/assets/missing.js")).hasStatus(404);
    }

    @Test
    void unknownApiPathKeepsTheApiAnswer() {
        MvcTestResult result = mockMvc.get().uri("/api/shop/nope").exchange();

        assertThat(result).hasStatus(404);
        assertNotIndexHtml(result);
    }

    @Test
    void unknownOpenApiPathIsNotIndexHtml() {
        MvcTestResult result = mockMvc.get().uri("/v3/nope").exchange();

        assertThat(result).hasStatus(404);
        assertNotIndexHtml(result);
    }

    private static void assertIndexHtml(MvcTestResult result) {
        assertThat(result).hasStatusOk().bodyText().contains(INDEX_HTML_MARKER);
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.TEXT_HTML_VALUE);
    }

    private static void assertNotIndexHtml(MvcTestResult result) {
        assertThat(result.getResponse().getContentType()).as(HttpHeaders.CONTENT_TYPE)
                .satisfiesAnyOf(type -> assertThat(type).isNull(), type -> assertThat(type).doesNotStartWith("text/html"));
        assertThat(result).bodyText().doesNotContain(INDEX_HTML_MARKER);
    }
}
