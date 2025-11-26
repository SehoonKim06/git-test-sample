package com.twolinecloud.MCP_gateway.openapi.schema;

/**
 * Validation Error Message
 * Customizable error message for validation failures
 */
public class ValidationErrorMessage {
    
    private final String fieldPath;      // 필드 경로 (예: "user.age")
    private final String errorCode;      // 에러 코드 (예: "required", "minimum")
    private final String message;        // 커스텀 메시지
    private final Object rejectedValue;  // 거부된 값
    
    public ValidationErrorMessage(String fieldPath, String errorCode, String message, Object rejectedValue) {
        this.fieldPath = fieldPath;
        this.errorCode = errorCode;
        this.message = message;
        this.rejectedValue = rejectedValue;
    }
    
    public String getFieldPath() {
        return fieldPath;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Object getRejectedValue() {
        return rejectedValue;
    }
    
    /**
     * Get formatted error message for API response
     */
    public String getFormattedMessage() {
        if (fieldPath != null && !fieldPath.isEmpty()) {
            return "'" + fieldPath + "': " + message;
        }
        return message;
    }
    
    /**
     * Convert to JSON-friendly format
     */
    public ValidationErrorResponse toResponse() {
        return new ValidationErrorResponse(
            fieldPath,
            errorCode,
            message,
            rejectedValue
        );
    }
    
    @Override
    public String toString() {
        return "ValidationError{" +
               "field='" + fieldPath + '\'' +
               ", code='" + errorCode + '\'' +
               ", message='" + message + '\'' +
               '}';
    }
}