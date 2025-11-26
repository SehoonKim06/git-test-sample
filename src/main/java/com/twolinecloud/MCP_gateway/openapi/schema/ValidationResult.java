package com.twolinecloud.MCP_gateway.openapi.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validation Result (Updated with custom error messages)
 */
public class ValidationResult {
    
    private final boolean valid;
    private final List<ValidationErrorMessage> errors;
    private final String path;
    
    private ValidationResult(boolean valid, List<ValidationErrorMessage> errors, String path) {
        this.valid = valid;
        this.errors = errors;
        this.path = path;
    }
    
    /**
     * Create success result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, new ArrayList<>(), "");
    }
    
    /**
     * Create failure result
     */
    public static ValidationResult failure(String errorMessage) {
        ValidationErrorMessage error = new ValidationErrorMessage(
            "",
            "validation.failed",
            errorMessage,
            null
        );
        List<ValidationErrorMessage> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors, "");
    }
    
    /**
     * Create failure with custom error
     */
    public static ValidationResult failure(ValidationErrorMessage error) {
        List<ValidationErrorMessage> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors, "");
    }
    
    /**
     * Create failure with field path and error code
     */
    public static ValidationResult failure(String fieldPath, String errorCode, String message, Object rejectedValue) {
        ValidationErrorMessage error = new ValidationErrorMessage(
            fieldPath,
            errorCode,
            message,
            rejectedValue
        );
        List<ValidationErrorMessage> errors = new ArrayList<>();
        errors.add(error);
        return new ValidationResult(false, errors, fieldPath);
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<ValidationErrorMessage> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public String getPath() {
        return path;
    }
    
    /**
     * Get first error message (simple string)
     */
    public String getFirstError() {
        return errors.isEmpty() ? null : errors.get(0).getMessage();
    }
    
    /**
     * Get first error with formatting
     */
    public String getFirstFormattedError() {
        return errors.isEmpty() ? null : errors.get(0).getFormattedMessage();
    }
    
    /**
     * Get all errors as formatted strings
     */
    public List<String> getAllFormattedErrors() {
        return errors.stream()
            .map(ValidationErrorMessage::getFormattedMessage)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all errors as response objects
     */
    public List<ValidationErrorResponse> getAllErrorResponses() {
        return errors.stream()
            .map(ValidationErrorMessage::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Get all errors as single string
     */
    public String getAllErrorsAsString() {
        if (valid) {
            return null;
        }
        return errors.stream()
            .map(ValidationErrorMessage::getFormattedMessage)
            .collect(Collectors.joining("; "));
    }
    
    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        }
        return "ValidationResult{valid=false, errors=" + getAllFormattedErrors() + "}";
    }
}


