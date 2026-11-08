package com.cleanlearning.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;

class RequiredResponseFieldsCustomizerTest {

    private final RequiredResponseFieldsCustomizer customizer = new RequiredResponseFieldsCustomizer();

    @Test
    void marksEveryResponseFieldRequired() {
        Schema<?> response = new ObjectSchema()
                .addProperty("name", new StringSchema())
                .addProperty("price", new Schema<>().$ref("#/components/schemas/MoneyResponse"));

        customise("ShopOfferingResponse", response);

        assertThat(response.getRequired()).containsExactly("name", "price");
    }

    @Test
    void leavesNullableResponseFieldsOptional() {
        Schema<?> response = new ObjectSchema()
                .addProperty("name", new StringSchema())
                .addProperty("bio", new Schema<>().types(Set.of("string", "null")))
                .addProperty("legacy", new StringSchema().nullable(true))
                .addProperty("photo", new Schema<>().oneOf(List.of(
                        new Schema<>().$ref("#/components/schemas/ImageResponse"),
                        new Schema<>().types(Set.of("null")))));

        customise("ExpertResponse", response);

        assertThat(response.getRequired()).containsExactly("name");
    }

    @Test
    void leavesOtherSchemasAlone() {
        Schema<?> request = new ObjectSchema().addProperty("note", new StringSchema());

        customise("AddNoteRequest", request);

        assertThat(request.getRequired()).isNull();
    }

    private void customise(String name, Schema<?> schema) {
        customizer.customise(new OpenAPI().components(new Components().addSchemas(name, schema)));
    }
}
