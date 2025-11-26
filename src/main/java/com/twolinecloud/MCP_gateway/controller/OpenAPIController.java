package com.twolinecloud.MCP_gateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.twolinecloud.MCP_gateway.openapi.parser.OpenAPIAnalyzer;
import com.twolinecloud.MCP_gateway.service.OpenAPIService;

@RestController
@RequestMapping("/api/openapi")
public class OpenAPIController {

    private final OpenAPIService openAPIService;

    public OpenAPIController(OpenAPIService openAPIService) {
        this.openAPIService = openAPIService;
    }

    /**
     * Gets OpenAPI Spec URL and returns analysis result
     */
    @GetMapping("/analyze")
    public ResponseEntity<OpenAPIAnalyzer.AnalysisResult> analyzeSpec(
            @RequestParam String url) {
        try {
            var result = openAPIService.processFromUrl(url);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Check health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}