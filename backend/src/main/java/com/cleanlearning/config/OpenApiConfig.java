package com.cleanlearning.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;

/**
 * The OpenAPI document at {@code /v3/api-docs}, which {@code backend/openapi.json} is generated from (D-29). The rest
 * of the setup (pretty-printed, sorted keys, JSON responses, nullable properties) is in {@code application.yml}.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Clean Learning Platform API").version("v1"))
                // A fixed server, so the document doesn't depend on the host it was requested from.
                // The SPA calls the API on its own origin.
                .servers(List.of(new Server().url("/")));
    }

    @Bean
    OpenApiCustomizer requiredResponseFields() {
        return new RequiredResponseFieldsCustomizer();
    }

    /**
     * {@code POST /api/logout} is Spring Security's logout filter, not a controller, so springdoc doesn't see it. The
     * document lists it, so the SPA calls it through the generated client like every other route.
     */
    @Bean
    OpenApiCustomizer logoutRoute() {
        return openApi -> openApi.path("/api/logout", new PathItem().post(new Operation()
                .operationId("logOut")
                .addTagsItem("logout")
                .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("No Content")))));
    }
}
