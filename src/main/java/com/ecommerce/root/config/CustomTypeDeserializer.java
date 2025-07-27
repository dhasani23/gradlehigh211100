package com.ecommerce.root.config;

import java.io.IOException;

/**
 * Custom type deserializer
 */
public class CustomTypeDeserializer {

    /**
     * Deserializes a custom type
     *
     * @param p The parser
     * @param ctxt The context
     * @return The deserialized object
     * @throws IOException If an I/O error occurs
     */
    public CustomType deserialize(Object p, Object ctxt) throws IOException {
        return new CustomType("");
    }
}