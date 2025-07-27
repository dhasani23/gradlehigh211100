package com.ecommerce.root.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Custom serializer for the CustomType class.
 * Demonstrates how to implement custom JSON serialization for a specific type.
 */
public class CustomTypeSerializer extends JsonSerializer<CustomType> {
    @Override
    public void serialize(CustomType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("custom_value", value.getValue());
        gen.writeStringField("serialized_at", java.time.LocalDateTime.now().toString());
        gen.writeEndObject();
    }
}