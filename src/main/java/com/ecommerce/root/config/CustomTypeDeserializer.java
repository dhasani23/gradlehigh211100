package com.ecommerce.root.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom deserializer for the CustomType class.
 * Demonstrates how to implement custom JSON deserialization for a specific type.
 */
public class CustomTypeDeserializer extends JsonDeserializer<CustomType> {
    @Override
    public CustomType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        String value = node.has("custom_value") ? 
                node.get("custom_value").asText() : 
                node.get("value").asText();
                
        return new CustomType(value);
    }
}