package com.twolinecloud.MCP_gateway.openapi.schema;

import java.util.List;
import java.util.Map;

/**
 * Phase 1.2: Complete Endpoint Schema Conversion Result
 * 
 * For each HTTP endpoint, we convert three types of schemas:
 * 1. Parameters Schema (query, path, header, cookie)
 * 2. RequestBody Schema (application/json content)
 * 3. Response Schema (response content - optional for Phase 1.2)
 */
public record EndpointSchemas(
    String path,
    String method,
    List<ConvertedParameter> parameters,
    ConvertedRequestBody requestBody,
    List<ConvertedResponse> responses
) {
    /**
     * Converted Parameter Metadata
     * From: parameters[] in OpenAPI
     * To: Java validation metadata
     */
    public record ConvertedParameter(
        String name,
        String location,        // path, query, header, cookie
        String javaType,        // Long, String, List<String>, etc.
        String description,
        ValidationMetadata validations,
        String format,
        boolean isArray,
        String arrayItemType
    ) {}
    
    /**
     * Converted Request Body Metadata
     * From: requestBody.content["application/json"].schema
     * To: Java validation metadata with nested structure
     */
    public record ConvertedRequestBody(
        String contentType,
        String javaType,
        String description,
        boolean required,
        ValidationSchema schema
    ) {}
    
    /**
     * Converted Response Metadata
     * From: responses[statusCode].content["application/json"].schema
     * To: Java validation metadata
     * (Optional for Phase 1.2)
     */
    public record ConvertedResponse(
        String statusCode,
        String contentType,
        String javaType,
        String description,
        ValidationSchema schema
    ) {}
    
    /**
     * Validation Metadata (for parameters and simple fields)
     */
    public record ValidationMetadata(
        Boolean required,
        Number minimum,
        Number maximum,
        Number exclusiveMinimum,
        Number exclusiveMaximum,
        Integer minLength,
        Integer maxLength,
        String pattern,
        List<?> enumValues,
        Integer minItems,
        Integer maxItems,
        Boolean uniqueItems
    ) {
        /**
         * Create empty validation metadata
         */
        public static ValidationMetadata empty() {
            return new ValidationMetadata(
                null, null, null, null, null,
                null, null, null, null, null, null, null
            );
        }
        
        /**
         * Check if any validation rule exists
         */
        public boolean hasValidations() {
            return required != null || minimum != null || maximum != null ||
                   exclusiveMinimum != null || exclusiveMaximum != null ||
                   minLength != null || maxLength != null || pattern != null ||
                   enumValues != null || minItems != null || maxItems != null ||
                   uniqueItems != null;
        }
    }
}