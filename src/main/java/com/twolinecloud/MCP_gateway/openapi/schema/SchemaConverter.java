package com.twolinecloud.MCP_gateway.openapi.schema;

import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIAnalyzer;
import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIAnalyzer.SchemaInfo;
import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIAnalyzer.ParameterInfo;
import com.twolinecloud.MCP_gateway.openapi.schema.validator.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Phase 1.2: Schema Converter (Revamped)
 * 
 * Boss 요구사항 (1순위):
 * OpenAPI 3.1 spec 기준으로 3가지 스키마 변환:
 * 1. Parameters 스키마 - query, path, header, cookie 파라미터
 * 2. RequestBody 스키마 - requestBody.content["application/json"].schema
 * 3. Response 스키마 - responses[statusCode].content["application/json"].schema (선택사항)
 * 
 * 각 스키마는 OpenAPI → Java Type + Validation 메타정보로 변환됩니다.
 */
@Component
public class SchemaConverter {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaConverter.class);
    
    // Cache for schema resolution
    private final ThreadLocal<Set<String>> processingSchemas = 
        ThreadLocal.withInitial(HashSet::new);
    private final Map<String, ValidationSchema> schemaCache = new HashMap<>();
    private OpenAPI openAPI;

    /**
     * Type mapping: OpenAPI type → Java type
     */
    private static final Map<String, String> TYPE_MAPPING = Map.of(
        "integer", "Integer",
        "number", "Double",
        "string", "String",
        "boolean", "Boolean",
        "array", "List",
        "object", "Object"
    );

    /**
     * Format-specific type mapping
     */
    private static final Map<String, String> FORMAT_TYPE_MAPPING = Map.of(
        "int32", "Integer",
        "int64", "Long",
        "float", "Float",
        "double", "Double",
        "byte", "byte[]",
        "binary", "byte[]",
        "date", "LocalDate",
        "date-time", "LocalDateTime",
        "uuid", "UUID",
        "uri", "URI"
    );

    /**
     * Set OpenAPI object for $ref resolution
     */
    public void setOpenAPI(OpenAPI openAPI) {
        this.openAPI = openAPI;
        log.info("✅ OpenAPI components loaded for $ref resolution");
    }

    // ========================================================================
    // ⭐ PUBLIC API - 3가지 스키마 변환 메서드
    // ========================================================================

    /**
     * ⭐ 1. CONVERT PARAMETERS
     * Convert OpenAPI parameters[] to Java validation metadata
     * 
     * @param parameters List of OpenAPI parameters
     * @return List of converted parameter metadata
     */
    public List<EndpointSchemas.ConvertedParameter> convertParameters(
            List<ParameterInfo> parameters) {
        
        if (parameters == null || parameters.isEmpty()) {
            return List.of();
        }

        List<EndpointSchemas.ConvertedParameter> result = new ArrayList<>();

        for (ParameterInfo param : parameters) {
            SchemaInfo schema = param.schema();
            if (schema == null) {
                log.warn("  ⚠️ Parameter '{}' has no schema, skipping", param.name());
                continue;
            }

            String type = schema.type();
            String format = schema.format();
            boolean isArray = "array".equals(type);
            
            String javaType;
            String arrayItemType = null;

            if (isArray && schema.items() != null) {
                SchemaInfo items = schema.items();
                arrayItemType = convertToJavaType(items.type(), items.format(), false);
                javaType = "List<" + arrayItemType + ">";
            } else {
                javaType = convertToJavaType(type, format, false);
            }

            // Extract validation metadata
            EndpointSchemas.ValidationMetadata validations = 
                extractValidationMetadata(schema, param.required());

            EndpointSchemas.ConvertedParameter converted = 
                new EndpointSchemas.ConvertedParameter(
                    param.name(),
                    param.in(),
                    javaType,
                    param.description(),
                    validations,
                    format,
                    isArray,
                    arrayItemType
                );

            result.add(converted);
            
            log.debug("    ✓ Parameter: {} ({}) -> {}", 
                param.name(), param.in(), javaType);
        }

        return result;
    }

    /**
     * ⭐ 2. CONVERT REQUEST BODY
     * Convert requestBody.content["application/json"].schema to Java metadata
     * 
     * @param requestBodySchema Request body schema info
     * @param required Whether request body is required
     * @param operationId Operation ID for naming
     * @return Converted request body metadata
     */
    public EndpointSchemas.ConvertedRequestBody convertRequestBody(
            SchemaInfo requestBodySchema, 
            boolean required,
            String operationId) {
        
        if (requestBodySchema == null) {
            return null;
        }

        String schemaName = operationId + ".requestBody";
        ValidationSchema schema = convert(requestBodySchema, schemaName);

        String javaType = convertToJavaType(
            requestBodySchema.type(), 
            requestBodySchema.format(), 
            false
        );

        log.debug("    ✓ RequestBody: {} (required: {})", javaType, required);

        return new EndpointSchemas.ConvertedRequestBody(
            "application/json",
            javaType,
            requestBodySchema.description(),
            required,
            schema
        );
    }

    /**
     * ⭐ 3. CONVERT RESPONSES (Optional for Phase 1.2)
     * Convert responses[statusCode].content["application/json"].schema
     * 
     * @param responseSchemas Map of status code to schema info
     * @param operationId Operation ID for naming
     * @return List of converted response metadata
     */
    public List<EndpointSchemas.ConvertedResponse> convertResponses(
            Map<String, SchemaInfo> responseSchemas,
            String operationId) {
        
        if (responseSchemas == null || responseSchemas.isEmpty()) {
            return List.of();
        }

        List<EndpointSchemas.ConvertedResponse> result = new ArrayList<>();

        for (Map.Entry<String, SchemaInfo> entry : responseSchemas.entrySet()) {
            String statusCode = entry.getKey();
            SchemaInfo responseSchema = entry.getValue();

            if (responseSchema == null) {
                continue;
            }

            String schemaName = operationId + ".response." + statusCode;
            ValidationSchema schema = convert(responseSchema, schemaName);

            String javaType = convertToJavaType(
                responseSchema.type(),
                responseSchema.format(),
                false
            );

            EndpointSchemas.ConvertedResponse converted = 
                new EndpointSchemas.ConvertedResponse(
                    statusCode,
                    "application/json",
                    javaType,
                    responseSchema.description(),
                    schema
                );

            result.add(converted);
            
            log.debug("    ✓ Response [{}] -> {}", statusCode, javaType);
        }

        return result;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Convert OpenAPI type + format to Java type
     */
    private String convertToJavaType(String type, String format, boolean isArray) {
        if (format != null && FORMAT_TYPE_MAPPING.containsKey(format)) {
            String javaType = FORMAT_TYPE_MAPPING.get(format);
            return isArray ? "List<" + javaType + ">" : javaType;
        }

        if (type == null) {
            return "Object";
        }

        String javaType = TYPE_MAPPING.getOrDefault(type, "Object");
        return isArray ? "List<" + javaType + ">" : javaType;
    }

    /**
     * Extract validation metadata from schema
     */
    private EndpointSchemas.ValidationMetadata extractValidationMetadata(
            SchemaInfo schema, 
            boolean required) {
        
        if (schema == null) {
            return EndpointSchemas.ValidationMetadata.empty();
        }

        // Note: Current SchemaInfo doesn't have all validation fields
        // This is a placeholder - you may need to enhance SchemaInfo
        // to include minimum, maximum, minLength, maxLength, pattern, enum, etc.
        
        return new EndpointSchemas.ValidationMetadata(
            required,
            null, // minimum - add to SchemaInfo if needed
            null, // maximum
            null, // exclusiveMinimum
            null, // exclusiveMaximum
            null, // minLength
            null, // maxLength
            null, // pattern
            null, // enum values
            null, // minItems
            null, // maxItems
            null  // uniqueItems
        );
    }

    // ========================================================================
    // INTERNAL SCHEMA CONVERSION
    // ========================================================================

    /**
     * Internal schema conversion - converts SchemaInfo to ValidationSchema
     * This method handles $ref resolution, caching, and circular reference detection
     */
    private ValidationSchema convert(SchemaInfo schemaInfo, String schemaName) {
        if (schemaInfo == null) {
            return new AnyValidator("No schema defined");
        }
        
        // Handle $ref first
        if (schemaInfo.ref() != null) {
            return resolveReference(schemaInfo.ref(), schemaName);
        }
        
        // Check cache
        if (schemaCache.containsKey(schemaName)) {
            log.debug("  ♻️ Using cached schema: {}", schemaName);
            return schemaCache.get(schemaName);
        }

        // Detect circular reference
        if (processingSchemas.get().contains(schemaName)) {
            log.debug("  ⚠️ Circular reference detected: {}", schemaName);
            return new ReferenceValidator(schemaName);
        }

        try {
            processingSchemas.get().add(schemaName);
            ValidationSchema schema = convertInternal(schemaInfo, schemaName);
            schemaCache.put(schemaName, schema);
            return schema;
        } finally {
            processingSchemas.get().remove(schemaName);
        }
    }

    /**
     * Resolve $ref to actual schema
     */
    private ValidationSchema resolveReference(String ref, String contextName) {
        String schemaName = extractSchemaName(ref);
        
        if (schemaCache.containsKey(ref)) {
            log.debug("  ♻️ Using cached $ref: {}", schemaName);
            return schemaCache.get(ref);
        }
        
        log.debug("  🔗 Resolving $ref: {}", schemaName);
        
        if (openAPI == null || openAPI.getComponents() == null) {
            log.error("  ❌ Cannot resolve $ref: OpenAPI components not available");
            return new AnyValidator("Unresolved reference: " + ref);
        }
        
        Schema<?> referencedSchema = openAPI.getComponents().getSchemas().get(schemaName);
        if (referencedSchema == null) {
            log.error("  ❌ Schema not found: {}", schemaName);
            return new AnyValidator("Schema not found: " + schemaName);
        }
        
        SchemaInfo resolvedSchemaInfo = extractSchemaInfoFromSwaggerSchema(referencedSchema);
        
        if (processingSchemas.get().contains(ref)) {
            log.debug("  ⚠️ Circular reference in $ref: {}", schemaName);
            return new ReferenceValidator(schemaName);
        }
        
        try {
            processingSchemas.get().add(ref);
            ValidationSchema converted = convertInternal(resolvedSchemaInfo, schemaName);
            schemaCache.put(ref, converted);
            return converted;
        } finally {
            processingSchemas.get().remove(ref);
        }
    }

    /**
     * Extract schema name from $ref path
     */
    private String extractSchemaName(String ref) {
        if (ref.contains("/")) {
            String[] parts = ref.split("/");
            return parts[parts.length - 1];
        }
        return ref;
    }

    /**
     * Convert Swagger Schema to SchemaInfo
     */
    private SchemaInfo extractSchemaInfoFromSwaggerSchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        
        String ref = schema.get$ref();
        if (ref != null) {
            return new SchemaInfo(null, null, null, null, null, null, ref);
        }
        
        String type = schema.getType();
        String format = schema.getFormat();
        
        if ("array".equals(type) && schema.getItems() != null) {
            SchemaInfo items = extractSchemaInfoFromSwaggerSchema(schema.getItems());
            return new SchemaInfo("array", format, null, items, null, 
                                 schema.getDescription(), null);
        }
        
        if ("object".equals(type) || (type == null && schema.getProperties() != null)) {
            Map<String, SchemaInfo> properties = new HashMap<>();
            if (schema.getProperties() != null) {
                schema.getProperties().forEach((name, prop) -> {
                    properties.put(name, 
                        extractSchemaInfoFromSwaggerSchema((Schema<?>) prop));
                });
            }
            return new SchemaInfo("object", format, properties, null, 
                                 schema.getRequired(), schema.getDescription(), null);
        }
        
        return new SchemaInfo(type, format, null, null, null, 
                             schema.getDescription(), null);
    }

    /**
     * Internal conversion logic - converts SchemaInfo to specific Validator
     */
    private ValidationSchema convertInternal(SchemaInfo schemaInfo, String schemaName) {
        String type = schemaInfo.type();
        
        if (type == null) {
            if (schemaInfo.properties() != null && !schemaInfo.properties().isEmpty()) {
                type = "object";
            } else if (schemaInfo.items() != null) {
                type = "array";
            } else {
                log.debug("  ⚠️ No type specified for: {}, using 'any'", schemaName);
                return new AnyValidator(schemaInfo.description());
            }
        }
        
        return switch (type.toLowerCase()) {
            case "string" -> new StringValidator(schemaInfo.format(), 
                                                 schemaInfo.description());
            case "number" -> new NumberValidator(schemaInfo.format(), 
                                                 schemaInfo.description());
            case "integer" -> new IntegerValidator(schemaInfo.format(), 
                                                   schemaInfo.description());
            case "boolean" -> new BooleanValidator(schemaInfo.description());
            case "array" -> convertArraySchema(schemaInfo, schemaName);
            case "object" -> convertObjectSchema(schemaInfo, schemaName);
            default -> {
                log.debug("  ⚠️ Unknown type: {}, using 'any'", type);
                yield new AnyValidator(schemaInfo.description());
            }
        };
    }

    /**
     * Convert array schema
     */
    private ValidationSchema convertArraySchema(SchemaInfo schemaInfo, String schemaName) {
        ValidationSchema itemsSchema = null;
        
        if (schemaInfo.items() != null) {
            String itemsSchemaName = schemaName + "[items]";
            itemsSchema = convert(schemaInfo.items(), itemsSchemaName);
        }
        
        return new ArrayValidator(itemsSchema, schemaInfo.description());
    }

    /**
     * Convert object schema
     */
    private ValidationSchema convertObjectSchema(SchemaInfo schemaInfo, String schemaName) {
        Map<String, ValidationSchema> properties = new HashMap<>();
        
        if (schemaInfo.properties() != null) {
            schemaInfo.properties().forEach((propName, propSchema) -> {
                String propSchemaName = schemaName + "." + propName;
                ValidationSchema validationSchema = convert(propSchema, propSchemaName);
                properties.put(propName, validationSchema);
            });
        }
        
        Set<String> requiredFields = new HashSet<>();
        if (schemaInfo.required() != null) {
            requiredFields.addAll(schemaInfo.required());
        }
        
        return new ObjectValidator(
            properties,
            requiredFields,
            schemaInfo.description(),
            true  // Allow additional properties by default
        );
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Clear schema cache
     */
    public void clearCache() {
        schemaCache.clear();
        log.info("Schema cache cleared");
    }

    /**
     * Cleanup thread-local resources
     */
    public void cleanup() {
        processingSchemas.remove();
    }
    
    /**
     * Print conversion summary
     */
    public void printConversionSummary() {
        log.info("Schema Conversion Summary");
        log.info("=".repeat(60));
        log.info("Total schemas converted: {}", schemaCache.size());
        
        // Count by type
        Map<String, Integer> typeCounts = new HashMap<>();
        schemaCache.values().forEach(schema -> {
            String type = schema.getType();
            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
        });
        
        log.info("Schemas by type:");
        typeCounts.forEach((type, count) -> 
            log.info("  {}: {}", type, count)
        );
        
        // Show resolved $refs
        long refCount = schemaCache.keySet().stream()
            .filter(key -> key.startsWith("#/components/schemas/"))
            .count();
        
        if (refCount > 0) {
            log.info("Resolved $ref schemas: {}", refCount);
        }
        log.info("");
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cacheSize", schemaCache.size());
        stats.put("cachedSchemas", new ArrayList<>(schemaCache.keySet()));
        return stats;
    }
}