package com.cleanlearning.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
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
}
