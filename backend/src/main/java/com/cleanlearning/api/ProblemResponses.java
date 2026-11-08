package com.cleanlearning.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.http.MediaType;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * The error answers a route declares in {@code openapi.json}, each with the shared {@code ProblemDetail} schema
 * (D-34). A controller method carries one annotation per error status it can answer from Spring MVC, such as
 * {@code @ProblemResponses.Status404}; a route without one lists no error. springdoc reads the {@code @ApiResponse}
 * on each annotation. 401, 403 and 500 stay status-only and out of the contract (D-34).
 * <p>
 * springdoc lists a route's success answer by itself only while the route declares no other answer, so a method with
 * one of these annotations also carries {@code @ResponseStatus} with its success status, such as
 * {@code @ResponseStatus(HttpStatus.OK)}. {@code OpenApiContractTest} checks that every route lists its success.
 */
public final class ProblemResponses {

    /** The name of the shared schema in {@code openapi.json}, which {@code ProblemDetailSchemaCustomizer} builds. */
    public static final String SCHEMA_NAME = "ProblemDetail";

    private static final String SCHEMA = "#/components/schemas/" + SCHEMA_NAME;

    private ProblemResponses() {
    }

    /** 400: a malformed id, an invalid or unreadable body, or a value that a value object rejects. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(ref = SCHEMA)))
    public @interface Status400 {
    }

    /** 404: the resource doesn't exist, or isn't the caller's. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(ref = SCHEMA)))
    public @interface Status404 {
    }

    /** 409: a business rule says no, with its {@code code}. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(ref = SCHEMA)))
    public @interface Status409 {
    }

    /** 415: a body of another media type than the route consumes. */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponse(responseCode = "415", description = "Unsupported Media Type", content = @Content(
            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(ref = SCHEMA)))
    public @interface Status415 {
    }
}
