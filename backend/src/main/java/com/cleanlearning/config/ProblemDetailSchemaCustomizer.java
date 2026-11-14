package com.cleanlearning.config;

import com.cleanlearning.api.ProblemResponses;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;

/**
 * The body as sent, not derived from Spring's {@code ProblemDetail} class: that one has a {@code
 * properties} map, which Jackson writes as top-level fields, and neither {@code errors} nor {@code
 * code}. {@code type} is optional: it's left out when it's {@code about:blank}.
 */
class ProblemDetailSchemaCustomizer implements OpenApiCustomizer {

  @Override
  public void customise(OpenAPI openApi) {
    openApi.getComponents().addSchemas(ProblemResponses.SCHEMA_NAME, problemDetail());
  }

  private static Schema<?> problemDetail() {
    return new ObjectSchema()
        .addProperty(
            "type", new StringSchema().format("uri").description("Left out when it's about:blank."))
        .addProperty("title", new StringSchema())
        .addProperty("status", new IntegerSchema())
        .addProperty("detail", new StringSchema())
        .addProperty("instance", new StringSchema().format("uri"))
        .addProperty(
            "errors",
            new ArraySchema()
                .items(invalidField())
                .description("A 400 from Bean Validation: each invalid field."))
        .addProperty(
            "code",
            new StringSchema()
                .description(
                    "A 409: the stable code of the business rule, like OFFERING_INCOMPLETE."))
        .required(List.of("instance", "status", "title"));
  }

  private static Schema<?> invalidField() {
    return new ObjectSchema()
        .addProperty(
            "field",
            new StringSchema()
                .description("The field's path as Spring reports it, like packages[1].price."))
        .addProperty("message", new StringSchema())
        .required(List.of("field", "message"));
  }
}
