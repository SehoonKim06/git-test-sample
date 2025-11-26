package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

import java.util.ArrayList;
import java.util.List;

/**
 * OneOf Validator
 * Value must match EXACTLY ONE of the schemas
 * 
 * Example: { "oneOf": [{"type": "string"}, {"type": "number"}] }
 * Valid: "hello" OR 123
 * Invalid: null (matches both) or object (matches none)
 */
public class OneOfValidator implements ValidationSchema {
    
    private final List<ValidationSchema> schemas;
    private final String description;
    
    public OneOfValidator(List<ValidationSchema> schemas, String description) {
        this.schemas = schemas != null ? schemas : new ArrayList<>();
        this.description = description;
    }
    
    @Override
    public String getType() {
        return "oneOf";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (schemas.isEmpty()) {
            return ValidationResult.failure("OneOf validator has no schemas defined");
        }
        
        int matchCount = 0;
        List<String> allErrors = new ArrayList<>();
        
        // Check how many schemas match
        for (int i = 0; i < schemas.size(); i++) {
            ValidationSchema schema = schemas.get(i);
            ValidationResult result = schema.validate(value);
            
            if (result.isValid()) {
                matchCount++;
            } else {
                allErrors.add("Schema [" + i + "]: " + result.getFirstError());
            }
        }
        
        // Must match exactly one
        if (matchCount == 0) {
            return ValidationResult.failure(
                "Value does not match any of the oneOf schemas. Errors: " + 
                String.join("; ", allErrors)
            );
        }
        
        if (matchCount > 1) {
            return ValidationResult.failure(
                "Value matches " + matchCount + " schemas, but oneOf requires exactly one match"
            );
        }
        
        return ValidationResult.success();
    }
    
    public List<ValidationSchema> getSchemas() {
        return new ArrayList<>(schemas);
    }
    
    @Override
    public String toString() {
        return "OneOfValidator{" +
               "schemas=" + schemas.size() +
               ", description='" + description + '\'' +
               '}';
    }
}
