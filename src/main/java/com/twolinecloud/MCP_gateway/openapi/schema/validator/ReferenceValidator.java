package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

public class ReferenceValidator implements ValidationSchema {
    
    private final String referenceName;
    private final String description;
    
    public ReferenceValidator(String referenceName) {
        this.referenceName = referenceName;
        this.description = "Reference to " + referenceName;
    }
    
    @Override
    public String getType() {
        return "reference";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        return ValidationResult.success();
    }
    
    public String getReferenceName() {
        return referenceName;
    }
}