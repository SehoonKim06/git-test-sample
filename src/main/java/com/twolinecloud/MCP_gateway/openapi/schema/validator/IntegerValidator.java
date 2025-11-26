package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

public class IntegerValidator implements ValidationSchema {
    
    private final String format;
    private final String description;
    
    public IntegerValidator(String format, String description) {
        this.format = format;
        this.description = description;
    }
    
    @Override
    public String getType() {
        return "integer";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getFormat() {
        return format;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (value == null) {
            return ValidationResult.success();
        }
        
        if (value instanceof Integer || value instanceof Long) {
            return ValidationResult.success();
        }
        
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (d == Math.floor(d)) {
                return ValidationResult.success();
            }
            return ValidationResult.failure(
                "Expected integer, but got decimal: " + d
            );
        }
        
        return ValidationResult.failure(
            "Expected integer, but got: " + value.getClass().getSimpleName()
        );
    }
}