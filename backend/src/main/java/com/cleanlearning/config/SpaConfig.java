package com.cleanlearning.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the SPA from {@code static/} (copied in by {@code ./mvnw -Pfrontend package}). A {@code GET} for a client
 * route such as {@code /offerings/<id>} gets {@code index.html}, so the route survives a reload. Existing files are
 * served as they are, a path whose last segment has a file extension (a missing asset) is a 404, and so is anything
 * under {@code /api} or {@code /v3}, which keeps the API's own answer.
 */
@Configuration(proxyBeanMethods = false)
class SpaConfig implements WebMvcConfigurer {

    private static final String STATIC_LOCATION = "classpath:/static/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations(STATIC_LOCATION)
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    static class SpaResourceResolver extends PathResourceResolver {

        private final Resource indexHtml = new ClassPathResource("static/index.html");

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Resource file = super.getResource(resourcePath, location);
            if (file != null) {
                return file;
            }
            return isClientRoute(resourcePath) && indexHtml.isReadable() ? indexHtml : null;
        }

        private static boolean isClientRoute(String resourcePath) {
            if (resourcePath.startsWith("api/") || resourcePath.startsWith("v3/")) {
                return false;
            }
            String lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            return !lastSegment.contains(".");
        }
    }
}
