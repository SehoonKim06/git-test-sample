package com.twolinecloud.MCP_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIAnalyzer;
import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIAnalyzer.EndpointInfo;
import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIDownloader;
import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIFileManager;
import com.twolinecloud.MCP_gateway.openapi.schema.EndpointSchemas;
import com.twolinecloud.MCP_gateway.openapi.schema.SchemaConverter;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 1.2: OpenAPI Service (Revamped)
 * 
 * Boss 요구사항 (1순위):
 * For each endpoint, converts three types of schemas:
 * 1. Parameters (query, path, header, cookie)
 * 2. RequestBody (application/json content)
 * 3. Responses (response content - optional for Phase 1.2)
 */
@Service
public class OpenAPIService {

    private static final Logger log = LoggerFactory.getLogger(OpenAPIService.class);
    
    private final OpenAPIDownloader downloader;
    private final OpenAPIAnalyzer analyzer;
    private final OpenAPIFileManager fileManager;
    private final SchemaConverter schemaConverter;

    public OpenAPIService(OpenAPIDownloader downloader, 
                          OpenAPIAnalyzer analyzer,
                          OpenAPIFileManager fileManager,
                          SchemaConverter schemaConverter) {
        this.downloader = downloader;
        this.analyzer = analyzer;
        this.fileManager = fileManager;
        this.schemaConverter = schemaConverter;
    }

    /**
     * ⭐ Phase 1.2: Download, analyze, and convert all endpoint schemas
     * 
     * @param url OpenAPI spec URL
     * @param fileName File name to save
     * @param includeResponses Whether to convert response schemas (optional for Phase 1.2)
     * @return Analysis result with converted schemas
     */
    public OpenAPIAnalysisWithSchemas downloadAndSaveWithSchemas(
            String url, 
            String fileName,
            boolean includeResponses) {
        
        // 1. Download
        log.info("📥 Downloading OpenAPI...");
        var downloadResult = downloader.downloadSpec(url);
        
        // 2. Save to file
        if (fileName == null || fileName.isEmpty()) {
            fileManager.saveSpecFromUrl(downloadResult.data(), url);
        } else {
            fileManager.saveSpec(downloadResult.data(), fileName);
        }
        
        // 3. Load OpenAPI for $ref resolution
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        OpenAPI openAPI = parser.readContents(downloadResult.data(), null, null).getOpenAPI();
        
        if (openAPI != null) {
            schemaConverter.setOpenAPI(openAPI);
        } else {
            log.warn("⚠️ Failed to load OpenAPI components - $ref resolution may not work");
        }
        
        // 4. Analyze
        var analysisResult = analyzer.analyze(downloadResult.data());
        analysisResult.printSummary();
        
        // 5. ⭐ Convert Schemas (Phase 1.2)
        log.info("🔄 Phase 1.2: Converting schemas to Java validation metadata...");
        log.info("   Include responses: {}", includeResponses);
        Map<String, EndpointSchemas> endpointSchemas = 
            convertAllEndpointSchemas(analysisResult, includeResponses);
        
        // 6. Print summary
        printSchemaConversionSummary(endpointSchemas, includeResponses);
        schemaConverter.printConversionSummary();
        
        return new OpenAPIAnalysisWithSchemas(analysisResult, endpointSchemas);
    }
    
    /**
     * Overload: Default to not including responses (Phase 1.2 requirement)
     */
    public OpenAPIAnalysisWithSchemas downloadAndSaveWithSchemas(
            String url, 
            String fileName) {
        return downloadAndSaveWithSchemas(url, fileName, false);
    }

    /**
     * ⭐ Convert all endpoint schemas
     * For each endpoint, convert:
     * 1. Parameters (query, path, header, cookie)
     * 2. RequestBody (application/json content)
     * 3. Responses (optional - controlled by includeResponses flag)
     */
    private Map<String, EndpointSchemas> convertAllEndpointSchemas(
            OpenAPIAnalyzer.AnalysisResult analysisResult,
            boolean includeResponses) {
        
        Map<String, EndpointSchemas> result = new HashMap<>();
        
        log.info("");
        log.info("Converting {} endpoints...", analysisResult.endpoints().size());
        
        for (EndpointInfo endpoint : analysisResult.endpoints()) {
            String endpointKey = endpoint.method() + " " + endpoint.path();
            String operationId = endpoint.operationId() != null ? 
                endpoint.operationId() : endpointKey;
            
            log.info("  📍 Converting: {}", endpointKey);
            
            // 1️⃣ Convert Parameters
            List<EndpointSchemas.ConvertedParameter> parameters = 
                schemaConverter.convertParameters(endpoint.parameters());
            
            if (!parameters.isEmpty()) {
                log.info("    ✓ Parameters: {} converted", parameters.size());
            }
            
            // 2️⃣ Convert RequestBody
            EndpointSchemas.ConvertedRequestBody requestBody = null;
            if (endpoint.requestBodySchema() != null) {
                // Note: In a real scenario, you'd check operation.requestBody.required
                // For now, we assume true
                requestBody = schemaConverter.convertRequestBody(
                    endpoint.requestBodySchema(),
                    true, // assume required by default
                    operationId
                );
                log.info("    ✓ RequestBody: converted ({})", requestBody.javaType());
            }
            
            // 3️⃣ Convert Responses (optional for Phase 1.2)
            List<EndpointSchemas.ConvertedResponse> responses = null;
            if (includeResponses && endpoint.responseSchemas() != null 
                && !endpoint.responseSchemas().isEmpty()) {
                responses = schemaConverter.convertResponses(
                    endpoint.responseSchemas(),
                    operationId
                );
                log.info("    ✓ Responses: {} converted", responses.size());
            }
            
            // Create EndpointSchemas
            EndpointSchemas schemas = new EndpointSchemas(
                endpoint.path(),
                endpoint.method(),
                parameters,
                requestBody,
                responses
            );
            
            result.put(endpointKey, schemas);
        }
        
        log.info("");
        return result;
    }

    /**
     * Print schema conversion summary
     */
    private void printSchemaConversionSummary(
            Map<String, EndpointSchemas> endpointSchemas,
            boolean includeResponses) {
        
        log.info("=".repeat(60));
        log.info("📊 Phase 1.2: Schema Conversion Summary");
        log.info("=".repeat(60));
        
        int totalParameters = 0;
        int totalRequestBodies = 0;
        int totalResponses = 0;
        
        for (EndpointSchemas schemas : endpointSchemas.values()) {
            if (schemas.parameters() != null) {
                totalParameters += schemas.parameters().size();
            }
            if (schemas.requestBody() != null) {
                totalRequestBodies++;
            }
            if (schemas.responses() != null) {
                totalResponses += schemas.responses().size();
            }
        }
        
        log.info("Total Endpoints: {}", endpointSchemas.size());
        log.info("  1️⃣ Parameters Converted: {}", totalParameters);
        log.info("  2️⃣ RequestBodies Converted: {}", totalRequestBodies);
        log.info("  3️⃣ Responses Converted: {} {}", totalResponses, 
            includeResponses ? "" : "(disabled - Phase 1.2 default)");
        
        log.info("");
        log.info("=".repeat(60));
    }

    /**
     * Analyze directly from URL without downloading
     */
    public OpenAPIAnalyzer.AnalysisResult processFromUrl(String url) {
        log.info("📥 Downloading: {}", url);
        var downloadResult = downloader.downloadSpec(url);
        
        log.info("🔍 Analyzing...");
        var analysisResult = analyzer.analyze(downloadResult.data());
        
        analysisResult.printSummary();
        return analysisResult;
    }

    /**
     * Analyze from a downloaded file
     */
    public OpenAPIAnalyzer.AnalysisResult processFromFile(String fileName) {
        log.info("📄 Reading file: {}", fileName);
        String content = fileManager.readSpec(fileName);
        
        log.info("🔍 Analyzing...");
        var analysisResult = analyzer.analyze(content);
        analysisResult.printSummary();
        
        return analysisResult;
    }
    
    /**
     * ⭐ Result class with schemas (Phase 1.2)
     */
    public record OpenAPIAnalysisWithSchemas(
        OpenAPIAnalyzer.AnalysisResult analysisResult,
        Map<String, EndpointSchemas> endpointSchemas
    ) {}
}