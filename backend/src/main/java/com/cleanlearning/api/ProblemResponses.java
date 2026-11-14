package com.cleanlearning.api;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

/**
 * springdoc lists a route's success answer by itself only while the route declares no other answer,
 * so a method with one of these annotations also carries {@code @ResponseStatus} with its success
 * status. {@code OpenApiContractTest} checks that every route lists its success. 401, 403 and 500
 * stay out of the contract (D-34).
 */
public final class ProblemResponses {

  public static final String SCHEMA_NAME = "ProblemDetail";

  private static final String SCHEMA = "#/components/schemas/" + SCHEMA_NAME;

  private ProblemResponses() {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(ref = SCHEMA)))
  public @interface Status400 {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(ref = SCHEMA)))
  public @interface Status404 {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponse(
      responseCode = "409",
      description = "Conflict",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(ref = SCHEMA)))
  public @interface Status409 {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @ApiResponse(
      responseCode = "415",
      description = "Unsupported Media Type",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
              schema = @Schema(ref = SCHEMA)))
  public @interface Status415 {}
}
