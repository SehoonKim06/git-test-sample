package com.twolinecloud.MCP_gateway.openapi.parser;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenAPI Spec Analyzer
 * Analyzes API end points, parameters and schemas
 */
@Component
public class OpenAPIAnalyzer {

	private static final Logger log = LoggerFactory.getLogger(OpenAPIAnalyzer.class);
    /**
     * Parse and analyze OpenAPI via string
     * 
     * @param specContent OpenAPI Spec descrip (JSON or YAML)
     * @return Analysis result
     */
    public AnalysisResult analyze(String specContent) {
        log.info("OpenAPI 스펙 분석 시작...");
        
        // Detect Version (Before parsing)
        String detectedVersion = detectVersionFromContent(specContent);
        log.info("감지된 스펙 형식: " + detectedVersion);

        // Parsing OpenAPI Spec (2.0 automatically converts to 3.0)
        SwaggerParseResult parseResult = new OpenAPIParser().readContents(specContent, null, null);
        OpenAPI openAPI = parseResult.getOpenAPI();
        
        if (openAPI == null) {
            log.error("파싱 실패 상세:");
            if (parseResult.getMessages() != null) {
                parseResult.getMessages().forEach(msg -> log.error("  - " + msg));
            }
            throw new OpenAPIAnalysisException(
                "OpenAPI 스펙 파싱 실패. 스펙 형식을 확인하세요."
            );
        }

        // Analysis
        //Number of end points
        List<EndpointInfo> endpoints = extractEndpoints(openAPI);
        Map<String, List<EndpointInfo>> groupedByMethod = groupByHttpMethod(endpoints);
        
        log.info("분석 완료: " + endpoints.size() + "개 엔드포인트 발견");

        return new AnalysisResult(
            openAPI.getInfo().getTitle(),
            openAPI.getInfo().getVersion(),
            endpoints,
            groupedByMethod
        );
    }

    /**
     * API List of end points
     * 
     * @param openAPI OpenAPI object
     * @return List of end points info
     */
    private List<EndpointInfo> extractEndpoints(OpenAPI openAPI) {
        List<EndpointInfo> endpoints = new ArrayList<>();

        if (openAPI.getPaths() == null) {
            return endpoints;
        }

        openAPI.getPaths().forEach((path, pathItem) -> {
        	//Extract Operation per HTTP methods
            extractOperation(path, "GET", pathItem.getGet(), endpoints);
            extractOperation(path, "POST", pathItem.getPost(), endpoints);
            extractOperation(path, "PUT", pathItem.getPut(), endpoints);
            extractOperation(path, "PATCH", pathItem.getPatch(), endpoints);
            extractOperation(path, "DELETE", pathItem.getDelete(), endpoints);
            extractOperation(path, "HEAD", pathItem.getHead(), endpoints);
            extractOperation(path, "OPTIONS", pathItem.getOptions(), endpoints);
        });

        return endpoints;
    }

    /**
     * Extract info per Operation
     */
    private void extractOperation(String path, String method, Operation operation, List<EndpointInfo> endpoints) {
        if (operation == null) {
            return;
        }

        // Extract parameter
        List<ParameterInfo> parameters = extractParameters(operation);

        // Extract request body
        SchemaInfo requestBodySchema = extractRequestBodySchema(operation);

        // Extract response schema
        Map<String, SchemaInfo> responseSchemas = extractResponseSchemas(operation);

        EndpointInfo endpoint = new EndpointInfo(
            path,
            method,
            operation.getOperationId(),
            operation.getSummary(),
            operation.getDescription(),
            operation.getTags(),
            parameters,
            requestBodySchema,
            responseSchemas
        );
        
        //print end point infos
        //endpoint.print();

        endpoints.add(endpoint);
    }

    /**
     * Extract parameter info
     */
    private List<ParameterInfo> extractParameters(Operation operation) {
        if (operation.getParameters() == null) {
            return new ArrayList<>();
        }

        return operation.getParameters().stream().map(param -> 
        	new ParameterInfo(
                param.getName(),
                param.getIn(), // query, header, path, cookie
                param.getRequired() != null ? param.getRequired() : false,
                param.getDescription(),
                extractSchemaInfo(param.getSchema())
            )
        ).collect(Collectors.toList());
    }

    /**
     * Extract RequestBody Schema
     */
    private SchemaInfo extractRequestBodySchema(Operation operation) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null) {
            return null;
        }

        MediaType mediaType = operation.getRequestBody().getContent().get("application/json");
        if (mediaType == null) {
            mediaType = operation.getRequestBody().getContent().values().stream()
                .findFirst()
                .orElse(null);
        }

        if (mediaType == null || mediaType.getSchema() == null) {
            return null;
        }

        return extractSchemaInfo(mediaType.getSchema());
    }

    /**
     * Analyze response schema
     */
    private Map<String, SchemaInfo> extractResponseSchemas(Operation operation) {
        Map<String, SchemaInfo> responseSchemas = new HashMap<>();

        if (operation.getResponses() == null) {
            return responseSchemas;
        }

        operation.getResponses().forEach((statusCode, response) -> {
            SchemaInfo schema = extractResponseSchema(response);
            if (schema != null) {
                responseSchemas.put(statusCode, schema);
            }
        });

        return responseSchemas;
    }

    /**
     * Extract each response schema
     */
    private SchemaInfo extractResponseSchema(ApiResponse response) {
        if (response.getContent() == null) {
            return null;
        }

        MediaType mediaType = response.getContent().get("application/json");
        if (mediaType == null) {
            mediaType = response.getContent().values().stream()
                .findFirst()
                .orElse(null);
        }

        if (mediaType == null || mediaType.getSchema() == null) {
            return null;
        }

        return extractSchemaInfo(mediaType.getSchema());
    }

    /**
     * Extract schema info
     */
    private SchemaInfo extractSchemaInfo(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        
        String ref = schema.get$ref();
        if (ref != null) {
            // If $ref exists, return immediately with just the reference
            return new SchemaInfo(
                null,  // type will be resolved later
                null,  // format
                null,  // properties
                null,  // items
                null,  // required
                schema.getDescription(),
                ref    // ⭐ Store $ref
            );
        }

        String type = schema.getType();
        String format = schema.getFormat();
        
        // Arrays
        if ("array".equals(type) && schema.getItems() != null) {
            SchemaInfo itemsSchema = extractSchemaInfo(schema.getItems());
            return new SchemaInfo(
                "array",
                format,
                null,
                itemsSchema,
                null,
                schema.getDescription(),
                null
            );
        }

        // Objects
        if ("object".equals(type) && schema.getProperties() != null) {
            Map<String, SchemaInfo> properties = new HashMap<>();
            schema.getProperties().forEach((propName, propSchema) -> {
                properties.put(propName, extractSchemaInfo((Schema<?>) propSchema));
            });

            return new SchemaInfo(
                "object",
                format,
                properties,
                null,
                schema.getRequired(),
                schema.getDescription(),
                null
            );
        }

        if (type == null && schema.getProperties() != null) {
            Map<String, SchemaInfo> properties = new HashMap<>();
            schema.getProperties().forEach((propName, propSchema) -> {
                properties.put(propName, extractSchemaInfo((Schema<?>) propSchema));
            });

            return new SchemaInfo(
                "object",  // Infer type as object
                format,
                properties,
                null,
                schema.getRequired(),
                schema.getDescription(),
                null  // no $ref
            );
        }

        // Basic types (string, number, integer, boolean)
        return new SchemaInfo(
            type,
            format,
            null,
            null,
            null,
            schema.getDescription(),
            null  // no $ref
        );
    }

    /**
     * Grouping HTTP methods
     */
    private Map<String, List<EndpointInfo>> groupByHttpMethod(List<EndpointInfo> endpoints) {
        return endpoints.stream()
            .collect(Collectors.groupingBy(EndpointInfo::method));
    }

    /**
     * Analysis Result
     */
    public record AnalysisResult(
        String apiTitle,
        String apiVersion,
        List<EndpointInfo> endpoints,
        Map<String, List<EndpointInfo>> groupedByMethod
    ) {
        public void printSummary() {
        	log.info("═══════════════════════════════════");
            log.info("API 분석 요약");
            log.info("═══════════════════════════════════");
            log.info("API 제목: " + apiTitle);
            log.info("버전: " + apiVersion);
            log.info("HTTP 메서드별 분포:");
            groupedByMethod.forEach((method, eps) -> {
                log.info("  " + method + ": " + eps.size() + "개");
            });
            log.info("═══════════════════════════════════");
        }
    }
    
    /**
     * Detect Spec Version
     */
    private String detectVersionFromContent(String specContent) {
        if (specContent == null || specContent.isEmpty()) {
            return "Unknown";
        }
        
        // Detect Swagger 2.0
        if ((specContent.substring(0, 20).contains("2.0"))
        		) {
            return "Swagger 2.0 (OpenAPI 2.0)";
        }
        
        // Detect OpenAPI 3.x
        if (specContent.substring(0, 20).contains("3.0")) {
                return "OpenAPI 3.0";
            } 
        if (specContent.substring(0, 20).contains("3.1")) {
                return "OpenAPI 3.1";
            }
        
        return "Unknown";
    }

    /**
     * End point Info
     */
    public record EndpointInfo(
        String path,
        String method,
        String operationId,
        String summary,
        String description,
        List<String> tags,
        List<ParameterInfo> parameters,
        SchemaInfo requestBodySchema,
        Map<String, SchemaInfo> responseSchemas
    ) {
        public void print() {
            log.info("🔹 " + method + " " + path);
            if (operationId != null) {
                log.info("   Operation ID: " + operationId);
            }
            if (summary != null) {
                log.info("   설명: " + summary);
            }
            if (tags != null && !tags.isEmpty()) {
                log.info("   태그: " + String.join(", ", tags));
            }
            if (!parameters.isEmpty()) {
                log.info("   파라미터: " + parameters.size() + "개");
                parameters.forEach(param -> 
                    log.info("     - " + param.name() + " (" + param.in() + ")")
                );
            }
            if (requestBodySchema != null) {
                log.info("   요청 본문: " + requestBodySchema.type());
            }
            if (!responseSchemas.isEmpty()) {
                log.info("   응답: " + responseSchemas.keySet());
            }
        }
    }

    /**
     * Parameter info
     */
    public record ParameterInfo(
        String name,
        String in,           // query, header, path, cookie
        boolean required,
        String description,
        SchemaInfo schema
    ) {}

    /**
     * Schema info
     */
    public record SchemaInfo(
        String type,                           // string, number, integer, boolean, array, object
        String format,                         // int32, int64, float, double, date, date-time, etc
        Map<String, SchemaInfo> properties,   // if object type
        SchemaInfo items,                      // if array type
        List<String> required,                 // list of required field
        String description,
        String ref,
        
        Number minimum,                        // minimum value
        Number maximum,                        // maximum value
        Number exclusiveMinimum,               // exclusive minimum
        Number exclusiveMaximum,               // exclusive maximum
        Integer minLength,                     // minimum string length
        Integer maxLength,                     // maximum string length
        String pattern,                        // regex pattern
        List<?> enumValues,                    // enum values
        Integer minItems,                      // minimum array items
        Integer maxItems,                      // maximum array items
        Boolean uniqueItems  
    ) {
    	public SchemaInfo(
    	        String type, 
    	        String format, 
    	        Map<String, SchemaInfo> properties,
    	        SchemaInfo items, 
    	        List<String> required, 
    	        String description
    	    ) {
    	        this(type, format, properties, 
    	             items, required, description, 
    	             null,
    	             null, null, null, null,  // ⭐ NEW
    	             null, null, null, null,  // ⭐ NEW
    	             null, null, null);       // ⭐ NEW
    	    }
    	    
    	    // ⭐ NEW: Constructor with ref
    	    public SchemaInfo(
    	        String type, 
    	        String format, 
    	        Map<String, SchemaInfo> properties,
    	        SchemaInfo items, 
    	        List<String> required, 
    	        String description, 
    	        String ref
    	    ) {
    	        this(type, format, properties, 
    	             items, required, description, 
    	             ref,
    	             null, null, null, null,  // ⭐ NEW
    	             null, null, null, null,  // ⭐ NEW
    	             null, null, null);       // ⭐ NEW
    	    }
    	}
    
    
    
    /**
     * Analysis Exception
     */
    public static class OpenAPIAnalysisException extends RuntimeException {
        public OpenAPIAnalysisException(String message) {
            super(message);
        }

        public OpenAPIAnalysisException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    
    
    
 
    // Print End points, HTTP methods grouping, parameters, schema infos and responses schema
    
    public static void printAnalyzedResults(AnalysisResult result) {
    	
        // HTTP 메서드별 그룹핑
        log.info("HTTP 메서드별 그룹핑:");
        log.info("═".repeat(80));
        result.groupedByMethod().forEach((method, endpoints) -> {
        	log.info("[" + method + "] - " + endpoints.size() + "개");
        	log.info("─".repeat(40));
         	endpoints.forEach(endpoint -> {
         		log.info("  • " + endpoint.path());
         	});
        });
        
        
        
        // HTTP 메서드별 정보
        
        log.info("HTTP 메서드별 정보:");
        log.info("═".repeat(80));
        result.endpoints().forEach(endpoint -> {
            log.info("[" + endpoint.method() + "] " + endpoint.path());
            log.info("─".repeat(40));
            
            // Description of the Method
            if (endpoint.summary() != null) {
    	        log.info("Description of method: " + endpoint.summary() + "");
    	    }
            
            // Parameters
            if (!endpoint.parameters().isEmpty()) {
                log.info("  Parameters:");
                endpoint.parameters().forEach(param -> {
                    String requiredMark = param.required() ? "* " : "  ";
                    log.info("    " + requiredMark + param.name() + 
                                     " (" + param.in() + ")");
                    
                    if (param.schema() != null) {
                        log.info("       Type: " + param.schema().type() + 
                                         (param.schema().format() != null ? 
                                          " (" + param.schema().format() + ")" : ""));
                    }
                    
                    if (param.description() != null) {
                        log.info("       Description: " + param.description());
                    }
                });
            }
            
            // Request Body Schema
            if (endpoint.requestBodySchema() != null) {
                log.info("  Request Body:");
                printSchema(endpoint.requestBodySchema(), "    ");
            }
            
            if (!endpoint.responseSchemas().isEmpty()) {
            	endpoint.responseSchemas().forEach((statusCode, schema) -> {
                    log.info("  Response [" + statusCode + "]:");
                    printSchema(schema, "    ");
                });
            }
        });
        
       
        log.info("" + "═".repeat(80));
        log.info("분석 완료!");
    	
    	
    }
    
    
    
    public static void printSchema(OpenAPIAnalyzer.SchemaInfo schema, String indent) {
        if (schema == null) {
            log.info(indent + "No schema");
            return;
        }
        
        log.info(indent + "Type: " + schema.type() + 
                         (schema.format() != null ? " (" + schema.format() + ")" : ""));
        
        if (schema.description() != null) {
            log.info(indent + "Description: " + schema.description());
        }
        
        // Array type
        if ("array".equals(schema.type()) && schema.items() != null) {
            log.info(indent + "Items:");
            printSchema(schema.items(), indent + "  ");
        }
        
        // Object type
        if ("object".equals(schema.type()) && schema.properties() != null) {
            log.info(indent + "Properties:");
            schema.properties().forEach((propName, propSchema) -> {
                String requiredMark = (schema.required() != null && 
                                      schema.required().contains(propName)) ? "* " : "  ";
                log.info(indent + "  " + requiredMark + propName + ":");
                printSchema(propSchema, indent + "    ");
            });
        }
    }
}
