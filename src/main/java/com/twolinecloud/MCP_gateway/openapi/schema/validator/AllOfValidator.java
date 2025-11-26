package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * AllOf Validator
 * Value must match ALL of the schemas
 * 
 * Example: { "allOf": [{"type": "object"}, {"required": ["name"]}] }
 * Valid: object with name field that satisfies both schemas
 * Invalid: object missing name, or non-object
 */
public class AllOfValidator implements ValidationSchema {
    
    private final List<ValidationSchema> schemas;
    private final String description;
    
    public AllOfValidator(List<ValidationSchema> schemas, String description) {
        this.schemas = schemas != null ? schemas : new ArrayList<>();
        this.description = description;
    }
    
    @Override
    public String getType() {
        return "allOf";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (schemas.isEmpty()) {
            return ValidationResult.failure("AllOf validator has no schemas defined");
        }
        
        // All schemas must match
        for (int i = 0; i < schemas.size(); i++) {
            ValidationSchema schema = schemas.get(i);
            ValidationResult result = schema.validate(value);
            
            if (!result.isValid()) {
                return ValidationResult.failure(
                    "AllOf validation failed at schema [" + i + "]: " + result.getFirstError()
                );
            }
        }
        
        // All schemas matched
        return ValidationResult.success();
    }
    
    public List<ValidationSchema> getSchemas() {
        return new ArrayList<>(schemas);
    }
    
    @Override
    public String toString() {
        return "AllOfValidator{" +
               "schemas=" + schemas.size() +
               ", description='" + description + '\'' +
               '}';
    }
}