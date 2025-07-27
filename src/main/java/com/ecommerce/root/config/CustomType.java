package com.ecommerce.root.config;

/**
 * Custom type for serialization
 */
public class CustomType {

    private String value;

    /**
     * Constructor
     *
     * @param value The value
     */
    public CustomType(String value) {
        this.value = value;
    }

    /**
     * Gets the value
     *
     * @return The value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value
     *
     * @param value The value
     */
    public void setValue(String value) {
        this.value = value;
    }
}