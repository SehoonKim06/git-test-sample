package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

public class NumberValidator implements ValidationSchema {
    
    private final String format;
    private final String description;
    
    public NumberValidator(String format, String description) {
        this.format = format;
        this.description = description;
    }
    
    @Override
    public String getType() {
        return "number";
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
        
        if (!(value instanceof Number)) {
            return ValidationResult.failure(
                "Expected number, but got: " + value.getClass().getSimpleName()
            );
        }
        
        return ValidationResult.success();
    }
}
