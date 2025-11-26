package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

public class AnyValidator implements ValidationSchema {
    
    private final String description;
    
    public AnyValidator(String description) {
        this.description = description;
    }
    
    @Override
    public String getType() {
        return "any";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        return ValidationResult.success();
    }
}
