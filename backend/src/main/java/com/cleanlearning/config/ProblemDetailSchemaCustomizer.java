package com.cleanlearning.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

/**
 * The one shared {@code ProblemDetail} schema of the error bodies (D-34). springdoc lists the answers of
 * {@code ApiExceptionHandler}'s {@code @ResponseStatus} methods on every route, with a schema it derives from Spring's
 * {@code ProblemDetail} class. That one doesn't match the JSON: it has a {@code properties} map, which Jackson writes
 * as top-level fields, and no {@code errors}. This customizer replaces it with the body as sent.
 * <p>
 * {@code type} is left out when it's {@code about:blank}, as every body of this API so far is, so it's optional.
 */
class ProblemDetailSchemaCustomizer implements OpenApiCustomizer {

    static final String NAME = "ProblemDetail";

    @Override
    public void customise(OpenAPI openApi) {
        openApi.getComponents().addSchemas(NAME, problemDetail());
    }

    private static Schema<?> problemDetail() {
        return new ObjectSchema()
                .addProperty("type", new StringSchema().format("uri")
                        .description("Left out when it's about:blank."))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema().format("uri"))
                .addProperty("errors", new ArraySchema()
                        .items(invalidField())
                        .description("A 400 from Bean Validation: each invalid field."))
                .required(List.of("instance", "status", "title"));
    }

    private static Schema<?> invalidField() {
        return new ObjectSchema()
                .addProperty("field", new StringSchema()
                        .description("The field's path as Spring reports it, like packages[1].price."))
                .addProperty("message", new StringSchema())
                .required(List.of("field", "message"));
    }
}
