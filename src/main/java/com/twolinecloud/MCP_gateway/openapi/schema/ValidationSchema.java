package com.twolinecloud.MCP_gateway.openapi.schema;

/**
 * Validation Schema Interface
 * All validators must implement this interface
 */
public interface ValidationSchema {
    
    /**
     * Get schema type
     * @return Type name (string, number, integer, boolean, array, object, any)
     */
    String getType();
    
    /**
     * Get schema description
     * @return Description
     */
    String getDescription();
    
    /**
     * Validate value
     * @param value Value to validate
     * @return Validation result
     */
    ValidationResult validate(Object value);
    
    /**
     * Get format (for string, number types)
     * @return Format (e.g., email, uuid, date, int32, int64, float, double)
     */
    default String getFormat() {
        return null;
    }
}