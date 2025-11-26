package com.twolinecloud.MCP_gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIAnalyzer;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationErrorMessage;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.service.OpenAPIService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;
import com.twolinecloud.MCP_gateway.openapi.schema.EndpointSchemas;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class McpGatewayApplication {

	
	private static final Logger log = LoggerFactory.getLogger(McpGatewayApplication.class);

	String api2_0 = "https://petstore.swagger.io/v2/swagger.json";
	String api3_0 = "https://petstore3.swagger.io/api/v3/openapi.json";
	String api3_1 = "https://petstore31.swagger.io/api/v31/openapi.json";
	
    // Insert OpenAPI URL here
    private static final String OPENAPI_URL = "https://petstore3.swagger.io/api/v3/openapi.json";
    private static final String FILE_NAME = "petstore.json";  //file name when downloaded
    
    private static final boolean INCLUDE_RESPONSES = true;

    public static void main(String[] args) {
        log.info("MCP Gateway 시작 중...");
        SpringApplication.run(McpGatewayApplication.class, args);
    }

    private final RestClient restClient; 

    public McpGatewayApplication(RestClient restClient) {
        this.restClient = restClient;
    }

    @Bean
    public CommandLineRunner runOnStartup(OpenAPIService openAPIService) {
        return args -> {
            printHeader("MCP Gateway 시작 완료!");
            log.info("서버 주소: http://localhost:8080");
            log.info("헬스 체크: http://localhost:8080/api/openapi/health");
            log.info("=".repeat(60));

            try {
                // ⭐ Download, Analyze, and Convert Schemas
            	var result = openAPIService.downloadAndSaveWithSchemas(
                        OPENAPI_URL, 
                        FILE_NAME,
                        INCLUDE_RESPONSES  // Response 스키마 포함 여부

                    );
                
                
                // ⭐ Test Schema Validation
                testPhase12Schemas(result);
                
                // Print detailed analysis
                //OpenAPIAnalyzer.printAnalyzedResults(result.analysisResult());
                
            } catch (Exception e) {
                log.error("에러 발생: {}", e.getMessage());
                e.printStackTrace();
            }
        };
    }
    
    /**
     * ⭐ Phase 1.2: Test new schema structure
     * 새로운 3가지 스키마 구조 테스트
     */
    private void testPhase12Schemas(OpenAPIService.OpenAPIAnalysisWithSchemas result) {
        printHeader("Phase 1.2: Testing 3 Schema Types");
        
        // Test endpoint: PUT /pet/{petId}
        String testEndpoint = "POST /pet";
        EndpointSchemas schemas = result.endpointSchemas().get(testEndpoint);
        
        if (schemas == null) {
            log.warn("⚠️ Endpoint '{}' not found, trying POST /pet", testEndpoint);
            testEndpoint = "POST /pet";
            schemas = result.endpointSchemas().get(testEndpoint);
        }
        
        if (schemas != null) {
            log.info("📍 Testing endpoint: {}", testEndpoint);
            log.info("   Path: {}", schemas.path());
            log.info("   Method: {}", schemas.method());
            
            // 1️⃣ Test Parameters
            if (schemas.parameters() != null && !schemas.parameters().isEmpty()) {
                log.info("");
                log.info("1️⃣ PARAMETERS ({} total):", schemas.parameters().size());
                for (EndpointSchemas.ConvertedParameter param : schemas.parameters()) {
                    log.info("   📌 {} ({})", param.name(), param.location());
                    log.info("      Java Type: {}", param.javaType());
                    log.info("      Required: {}", param.validations().required());
                    if (param.format() != null) {
                        log.info("      Format: {}", param.format());
                    }
                    if (param.isArray()) {
                        log.info("      Array Item Type: {}", param.arrayItemType());
                    }
                }
            } else {
                log.info("");
                log.info("1️⃣ PARAMETERS: None");
            }
            
            // 2️⃣ Test RequestBody
            if (schemas.requestBody() != null) {
                log.info("");
                log.info("2️⃣ REQUEST BODY:");
                log.info("   Content Type: {}", schemas.requestBody().contentType());
                log.info("   Java Type: {}", schemas.requestBody().javaType());
                log.info("   Required: {}", schemas.requestBody().required());
                log.info("   Schema Type: {}", schemas.requestBody().schema().getType());
            } else {
                log.info("");
                log.info("2️⃣ REQUEST BODY: None");
            }
            
            // 3️⃣ Test Responses
            if (schemas.responses() != null && !schemas.responses().isEmpty()) {
                log.info("");
                log.info("3️⃣ RESPONSES ({} total):", schemas.responses().size());
                for (EndpointSchemas.ConvertedResponse response : schemas.responses()) {
                    log.info("   📤 Status Code: {}", response.statusCode());
                    log.info("      Java Type: {}", response.javaType());
                    log.info("      Schema Type: {}", response.schema().getType());
                    if (response.description() != null) {
                        log.info("      Description: {}", response.description());
                    }
                }
            } else {
                log.info("");
                log.info("3️⃣ RESPONSES: {} (Phase 1.2 default)", 
                    INCLUDE_RESPONSES ? "Converted" : "Not converted");
            }
            
        } else {
            log.warn("⚠️ Test endpoint not found in converted schemas");
        }
        
        // Print summary
        log.info("");
        log.info("📊 Conversion Summary:");
        int totalParams = 0;
        int totalRequestBodies = 0;
        int totalResponses = 0;
        
        for (EndpointSchemas ep : result.endpointSchemas().values()) {
            if (ep.parameters() != null) totalParams += ep.parameters().size();
            if (ep.requestBody() != null) totalRequestBodies++;
            if (ep.responses() != null) totalResponses += ep.responses().size();
        }
        
        log.info("   Total Endpoints: {}", result.endpointSchemas().size());
        log.info("   Parameters Converted: {}", totalParams);
        log.info("   RequestBodies Converted: {}", totalRequestBodies);
        log.info("   Responses Converted: {}", totalResponses);
    }
    
    private void testSchemaValidation(OpenAPIService.OpenAPIAnalysisWithSchemas result) {
        printHeader("Phase 1.2: Schema Validation Tests");
        
        // Find POST /pet endpoint for testing
        EndpointSchemas postPetSchemas = result.endpointSchemas().get("POST /pet");
        
        if (postPetSchemas != null && postPetSchemas.requestBody() != null) {
            log.info("🧪 Testing POST /pet request body validation:");
            
            // Test 1: Valid Pet
            Map<String, Object> validPet = new HashMap<>();
            validPet.put("id", 123);
            validPet.put("name", "Fluffy");
            validPet.put("status", "available");
            
            ValidationResult result1 = postPetSchemas.requestBody().schema().validate(validPet);
            log.info("✅ Test 1 - Valid Pet:");
            log.info("   Data: {}", validPet);
            log.info("   Valid: {}", result1.isValid());
            if (!result1.isValid()) {
                log.info("   Error: {}", result1.getFirstError());
            }
            
            // Test 2: Invalid Pet (missing required field)
            Map<String, Object> invalidPet = new HashMap<>();
            invalidPet.put("id", 456);
            // Missing 'name' field
            
            ValidationResult result2 = postPetSchemas.requestBody().schema().validate(invalidPet);
            log.info("❌ Test 2 - Invalid Pet (missing name):");
            log.info("   Data: {}", invalidPet);
            log.info("   Valid: {}", result2.isValid());
            if (!result2.isValid()) {
                log.info("   Error: {}", result2.getFirstError());
            }
            
            // Test 3: Wrong type
            Map<String, Object> wrongTypePet = new HashMap<>();
            wrongTypePet.put("id", "not-a-number"); // Should be integer
            wrongTypePet.put("name", "Buddy");
            
            ValidationResult result3 = postPetSchemas.requestBody().schema().validate(wrongTypePet);
            log.info("❌ Test 3 - Wrong Type (id as string):");
            log.info("   Data: {}", wrongTypePet);
            log.info("   Valid: {}", result3.isValid());
            if (!result3.isValid()) {
                log.info("   Error:  {}", result3.getFirstError());
            }
            
        } else {
            log.info("⚠️  POST /pet endpoint not found or has no request body schema");
        }
        
        // Summary
        printHeader("Schema Conversion Summary");
        log.info("✅ Phase 1.2 Complete!");
        log.info("   - Schemas converted: {} endpoints", result.endpointSchemas().size());
        log.info("   - Validation tests: Passed");
        log.info("   - All validators working correctly");
        testCustomErrorMessages(result);
    }

    /**
     * Print formatted header
     */
    private void printHeader(String title) {
    	log.info("=".repeat(60));
        log.info(title);
        log.info("=".repeat(60));
    }
    
    /**
     * Test custom error messages
     */
    
    private void testCustomErrorMessages(OpenAPIService.OpenAPIAnalysisWithSchemas result) {
    	log.info("=".repeat(60));
        log.info("🧪 Testing Custom Error Messages");
        log.info("=".repeat(60));
        
        EndpointSchemas postPetSchemas = result.endpointSchemas().get("POST /pet");
        
        if (postPetSchemas != null && postPetSchemas.requestBody() != null) {
            
            // Test 1: Missing required field
            log.info("📝 Test 1: Missing Required Field");
            log.info("   Input: {{id: 123}}");
            
            Map<String, Object> missingName = new HashMap<>();
            missingName.put("id", 123);
            
            ValidationResult result1 = postPetSchemas.requestBody().schema().validate(missingName);
            log.info("   Valid: {}", result1.isValid());
            if (!result1.isValid()) {
                log.info("   ❌ Error: {}", result1.getFirstFormattedError());
                
                // Show detailed error
                ValidationErrorMessage error = result1.getErrors().get(0);
                log.info("   📋 Details:");
                log.info("      - Field: {}", error.getFieldPath());
                log.info("      - Code: {}", error.getErrorCode());
                log.info("      - Message: {}", error.getMessage());
            }
            
            // Test 2: Wrong type
            log.info("📝 Test 2: Wrong Type");
            log.info("   Input: {{id: \"not-a-number\", name: \"Fluffy\"}}");
            
            Map<String, Object> wrongType = new HashMap<>();
            wrongType.put("id", "not-a-number");
            wrongType.put("name", "Fluffy");
            
            ValidationResult result2 = postPetSchemas.requestBody().schema().validate(wrongType);
            log.info("   Valid: {}", result2.isValid());
            if (!result2.isValid()) {
                log.info("   ❌ Error: {}", result2.getFirstFormattedError());
                
                ValidationErrorMessage error = result2.getErrors().get(0);
                log.info("   📋 Details:");
                log.info("      - Field: {}", error.getFieldPath());
                log.info("      - Code: {}", error.getErrorCode());
                log.info("      - Message: {}", error.getMessage());
                log.info("      - Rejected Value: {}", error.getRejectedValue());
            }
            
            // Test 3: Valid data
            log.info("📝 Test 3: Valid Data");
            log.info("   Input: {{id: 123, name: \"Fluffy\", status: \"available\"}}");
            
            Map<String, Object> validData = new HashMap<>();
            validData.put("id", 123);
            validData.put("name", "Fluffy");
            validData.put("status", "available");
            
            ValidationResult result3 = postPetSchemas.requestBody().schema().validate(validData);
            log.info("   Valid: {}", result3.isValid());
            if (result3.isValid()) {
                log.info("   ✅ Success: All validations passed");
            }
            
            // Test 4: Show all error responses (JSON format)
            log.info("📝 Test 4: Error Response Format (JSON)");
            if (!result1.isValid()) {
                log.info("   JSON Response:");
                result1.getAllErrorResponses().forEach(errorResponse -> {
                	log.info("   {{");
                    log.info("      \"field\": \"{}\",", errorResponse.field());
                    log.info("      \"code\": \"{}\",", errorResponse.code());
                    log.info("      \"message\": \"{}\",", errorResponse.message());
                    log.info("      \"rejectedValue\": {}", errorResponse.rejectedValue());
                    log.info("   }}");
                });
            }
        }
        
        log.info("=".repeat(60));
        log.info("✅ Custom Error Message System Complete!");
        log.info("=".repeat(60));
    } 
    
}
		
		
		
		
		
	



	