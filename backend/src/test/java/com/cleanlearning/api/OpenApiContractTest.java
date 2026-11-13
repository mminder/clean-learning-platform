package com.cleanlearning.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.assertj.core.api.AbstractMapAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.cleanlearning.IntegrationTest;

/**
 * The committed {@code backend/openapi.json} is the API contract the frontend types are generated from (D-29). It
 * must match the document the code generates. With {@code -Dopenapi.write=true}, the test writes the file instead.
 * Each route lists the error answers it can give, and only those ({@code ProblemResponses}).
 */
@IntegrationTest
class OpenApiContractTest {

    private static final Path COMMITTED = Path.of("openapi.json");
    private static final String REGENERATE = "./mvnw test -Dtest=OpenApiContractTest -Dopenapi.write=true";

    @Autowired
    MockMvcTester mockMvc;

    @Test
    void committedDocumentMatchesTheCode() throws IOException {
        MvcTestResult result = mockMvc.get().uri("/v3/api-docs").exchange();
        assertThat(result).hasStatusOk();
        String generated = result.getResponse().getContentAsString(StandardCharsets.UTF_8) + "\n";

        if (Boolean.getBoolean("openapi.write")) {
            Files.writeString(COMMITTED, generated);
            return;
        }

        assertThat(COMMITTED)
                .as("backend/openapi.json is missing. Generate it in backend/ with: " + REGENERATE)
                .exists();
        assertThat(Files.readString(COMMITTED))
                .as("backend/openapi.json is out of date. Regenerate it in backend/ with: " + REGENERATE
                        + ", then run npm run generate:api in frontend/")
                .isEqualTo(generated);
    }

    @Test
    void aRouteListsOnlyTheErrorsItCanAnswer() {
        assertResponsesOf("/api/shop/offerings", "get").containsOnlyKeys("200");
        assertResponsesOf("/api/admin/consultation-offerings", "get").containsOnlyKeys("200");
        assertResponsesOf("/api/me", "get").containsOnlyKeys("200", "404");
        assertResponsesOf("/api/shop/offerings/{id}", "get").containsOnlyKeys("200", "400", "404");
        assertResponsesOf("/api/admin/consultation-offerings", "post").containsOnlyKeys("201", "400", "415");
        assertResponsesOf("/api/admin/consultation-offerings/{id}/publish", "post")
                .containsOnlyKeys("200", "400", "404", "409");
        assertResponsesOf("/api/customer/consultation-cases", "post")
                .containsOnlyKeys("201", "400", "404", "409", "415");
        assertResponsesOf("/api/customer/consultation-cases/{id}", "get").containsOnlyKeys("200", "400", "404");
        assertResponsesOf("/api/customer/consultation-cases/{id}/eligible-experts", "get")
                .containsOnlyKeys("200", "400", "404");
        assertResponsesOf("/api/customer/consultation-cases/{id}/bookable-slots", "get")
                .containsOnlyKeys("200", "400", "404", "409");
        // 201 without a Location: an appointment has no route of its own (spec 04, decision 17)
        assertResponsesOf("/api/customer/consultation-cases/{id}/appointments", "post")
                .containsOnlyKeys("201", "400", "404", "409", "415");
        assertResponsesOf("/api/customer/consultation-cases/{id}/appointments/{appointmentId}/cancel", "post")
                .containsOnlyKeys("200", "400", "404", "409", "415");
        assertResponsesOf("/api/customer/consultation-cases/{id}/cancel", "post")
                .containsOnlyKeys("200", "400", "404", "409", "415");
        assertResponsesOf("/api/expert/availability", "put").containsOnlyKeys("200", "400", "415");
    }

    @Test
    void anErrorAnswerHasTheSharedProblemDetailSchema() {
        assertThat(mockMvc.get().uri("/v3/api-docs")).bodyJson()
                .extractingPath("$.paths['/api/admin/consultation-offerings/{id}/publish'].post.responses['409']"
                        + ".content['application/problem+json'].schema['$ref']")
                .isEqualTo("#/components/schemas/ProblemDetail");
    }

    /** springdoc lists a route's success by itself only while the route declares no error answer. */
    @Test
    void everyRouteListsItsSuccess() {
        assertThat(mockMvc.get().uri("/v3/api-docs")).bodyJson()
                .extractingPath("$.paths.*.*.responses").asArray().isNotEmpty()
                .allSatisfy(responses -> assertThat(((Map<?, ?>) responses).keySet())
                        .anyMatch(code -> code.toString().startsWith("2")));
    }

    private AbstractMapAssert<?, ?, String, Object> assertResponsesOf(String path, String method) {
        return assertThat(mockMvc.get().uri("/v3/api-docs")).bodyJson()
                .extractingPath("$.paths['" + path + "']." + method + ".responses").asMap();
    }
}
