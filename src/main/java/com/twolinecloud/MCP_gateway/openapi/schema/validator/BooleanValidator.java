package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

public class BooleanValidator implements ValidationSchema {
    
    private final String description;
    
    public BooleanValidator(String description) {
        this.description = description;
    }
    
    @Override
    public String getType() {
        return "boolean";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (value == null) {
            return ValidationResult.success();
        }
        
        if (!(value instanceof Boolean)) {
            return ValidationResult.failure(
                "Expected boolean, but got: " + value.getClass().getSimpleName()
            );
        }
        
        return ValidationResult.success();
    }
}