package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * AnyOf Validator
 * Value must match AT LEAST ONE of the schemas
 * 
 * Example: { "anyOf": [{"type": "string"}, {"type": "number"}] }
 * Valid: "hello" OR 123 OR both
 * Invalid: object (matches none)
 */
public class AnyOfValidator implements ValidationSchema {
    
    private final List<ValidationSchema> schemas;
    private final String description;
    
    public AnyOfValidator(List<ValidationSchema> schemas, String description) {
        this.schemas = schemas != null ? schemas : new ArrayList<>();
        this.description = description;
    }
    
    @Override
    public String getType() {
        return "anyOf";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (schemas.isEmpty()) {
            return ValidationResult.failure("AnyOf validator has no schemas defined");
        }
        
        List<String> allErrors = new ArrayList<>();
        
        // Check if at least one schema matches
        for (int i = 0; i < schemas.size(); i++) {
            ValidationSchema schema = schemas.get(i);
            ValidationResult result = schema.validate(value);
            
            if (result.isValid()) {
                // At least one match found, success!
                return ValidationResult.success();
            } else {
                allErrors.add("Schema [" + i + "]: " + result.getFirstError());
            }
        }
        
        // No schema matched
        return ValidationResult.failure(
            "Value does not match any of the anyOf schemas. Errors: " + 
            String.join("; ", allErrors)
        );
    }
    
    public List<ValidationSchema> getSchemas() {
        return new ArrayList<>(schemas);
    }
    
    @Override
    public String toString() {
        return "AnyOfValidator{" +
               "schemas=" + schemas.size() +
               ", description='" + description + '\'' +
               '}';
    }
}
