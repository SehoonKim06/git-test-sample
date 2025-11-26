package com.twolinecloud.MCP_gateway.openapi.schema;

/**
 * Validation Error Response (JSON format)
 * 공통 에러 Response 포맷
 */
public record ValidationErrorResponse(
    String field,          // 필드명
    String code,           // 에러 코드
    String message,        // 에러 메시지
    Object rejectedValue   // 거부된 값
) {}