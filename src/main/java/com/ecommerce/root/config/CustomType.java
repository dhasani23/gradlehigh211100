package com.ecommerce.root.config;

/**
 * Placeholder class used for custom serialization/deserialization in WebConfig.
 * This class would typically represent a domain-specific data type
 * that requires special handling during JSON conversion.
 */
public class CustomType {
    private String value;
    
    public CustomType() {
    }
    
    public CustomType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
}