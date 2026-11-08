package com.cleanlearning.config;

import org.springdoc.core.customizers.OpenApiCustomizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Marks every property of a response schema (a {@code *Response} record) as required, unless it's nullable. A
 * response always contains all its fields, so the generated TypeScript types say {@code name: string}, not
 * {@code name?: string}. A response field that may be null is annotated with JSpecify's {@code @Nullable}, which
 * springdoc turns into a nullable property. Request schemas keep what Bean Validation says.
 */
class RequiredResponseFieldsCustomizer implements OpenApiCustomizer {

    private static final String NULL_TYPE = "null";

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (name.endsWith("Response") && schema.getProperties() != null) {
                markRequired(schema);
            }
        });
    }

    private static void markRequired(Schema<?> schema) {
        // addRequiredItem skips duplicates and keeps the list sorted.
        schema.getProperties().forEach((propertyName, property) -> {
            if (!isNullable(property)) {
                schema.addRequiredItem(propertyName);
            }
        });
    }

    private static boolean isNullable(Schema<?> property) {
        if (Boolean.TRUE.equals(property.getNullable())) {
            return true;
        }
        if (property.getTypes() != null && property.getTypes().contains(NULL_TYPE)) {
            return true;
        }
        return property.getOneOf() != null && property.getOneOf().stream().anyMatch(alternative -> NULL_TYPE
                .equals(alternative.getType())
                || (alternative.getTypes() != null && alternative.getTypes().contains(NULL_TYPE)));
    }
}
