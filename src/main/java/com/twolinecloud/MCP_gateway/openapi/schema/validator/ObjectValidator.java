package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import com.twolinecloud.MCP_gateway.openapi.schema.ErrorMessageTemplate;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationErrorMessage;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

import java.util.*;

/**
 * Object Type Validator (Updated with Custom Error Messages)
 * Validates object/map values and their properties
 */
public class ObjectValidator implements ValidationSchema {
    
    private final Map<String, ValidationSchema> properties;
    private final Set<String> requiredFields;
    private final String description;
    private final Boolean additionalPropertiesAllowed;
    
    public ObjectValidator(Map<String, ValidationSchema> properties,
                          Set<String> requiredFields,
                          String description) {
        this(properties, requiredFields, description, true);
    }
    
    public ObjectValidator(Map<String, ValidationSchema> properties,
                          Set<String> requiredFields,
                          String description,
                          Boolean additionalPropertiesAllowed) {
        this.properties = properties != null ? properties : new HashMap<>();
        this.requiredFields = requiredFields != null ? requiredFields : new HashSet<>();
        this.description = description;
        this.additionalPropertiesAllowed = additionalPropertiesAllowed;
    }
    
    @Override
    public String getType() {
        return "object";
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
        
        // Type check with custom message
        if (!(value instanceof Map)) {
            String message = ErrorMessageTemplate.getTemplate("type.object");
            return ValidationResult.failure(
                "",
                "type.mismatch",
                message + " (받은 타입: " + value.getClass().getSimpleName() + ")",
                value
            );
        }
        
        Map<?, ?> map = (Map<?, ?>) value;
        
        // Validate required fields with custom messages
        for (String requiredField : requiredFields) {
            if (!map.containsKey(requiredField)) {
                String message = ErrorMessageTemplate.getTemplate("required");
                return ValidationResult.failure(
                    requiredField,
                    "required",
                    message,
                    null
                );
            }
            
            Object fieldValue = map.get(requiredField);
            if (fieldValue == null) {
                String message = ErrorMessageTemplate.getTemplate("required");
                return ValidationResult.failure(
                    requiredField,
                    "required",
                    message + " (null 값은 허용되지 않습니다)",
                    null
                );
            }
        }
        
        // Validate each property with custom messages
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String propName = entry.getKey().toString();
            Object propValue = entry.getValue();
            
            // Check if property is defined in schema
            if (!properties.containsKey(propName)) {
                if (Boolean.FALSE.equals(additionalPropertiesAllowed)) {
                    String message = ErrorMessageTemplate.getTemplate("object.additionalProperties");
                    return ValidationResult.failure(
                        propName,
                        "additionalProperties",
                        message,
                        propValue
                    );
                }
                continue;
            }
            
            // Validate property value
            ValidationSchema propSchema = properties.get(propName);
            ValidationResult propResult = propSchema.validate(propValue);
            
            if (!propResult.isValid()) {
                // Add field path to error
                ValidationErrorMessage originalError = propResult.getErrors().get(0);
                return ValidationResult.failure(
                    propName,
                    originalError.getErrorCode(),
                    originalError.getMessage(),
                    propValue
                );
            }
        }
        
        return ValidationResult.success();
    }
    
    public Map<String, ValidationSchema> getProperties() {
        return new HashMap<>(properties);
    }
    
    public Set<String> getRequiredFields() {
        return new HashSet<>(requiredFields);
    }
    
    @Override
    public String toString() {
        return "ObjectValidator{" +
               "properties=" + properties.size() +
               ", requiredFields=" + requiredFields +
               ", description='" + description + '\'' +
               '}';
    }
}