package com.twolinecloud.MCP_gateway.openapi.schema.validator;

import java.util.Map;

import com.twolinecloud.MCP_gateway.openapi.schema.ErrorMessageTemplate;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationResult;
import com.twolinecloud.MCP_gateway.openapi.schema.ValidationSchema;

/**
 * String Type Validator
 * Validates string values and formats (email, uuid, date, etc.)
 */
public class StringValidator implements ValidationSchema {
    
    private final String format;
    private final String description;
    private final Integer minLength;
    private final Integer maxLength;
    private final String pattern;
    
    public StringValidator(String format, String description) {
        this(format, description, null, null, null);
    }
    
    public StringValidator(String format, String description, 
                          Integer minLength, Integer maxLength, String pattern) {
        this.format = format;
        this.description = description;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.pattern = pattern;
    }
    
    @Override
    public String getType() {
        return "string";
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getFormat() {
        return format;
    }
    
    @Override
    public ValidationResult validate(Object value) {
        if (value == null) {
            return ValidationResult.success();
        }
        
        // Type check with custom message
        if (!(value instanceof String)) {
            String message = ErrorMessageTemplate.getTemplate("type.string");
            return ValidationResult.failure(
                "",
                "type.mismatch",
                message + " (받은 값: " + value.getClass().getSimpleName() + ")",
                value
            );
        }
        
        String strValue = (String) value;
        
        // Length validation with custom message
        if (minLength != null && strValue.length() < minLength) {
            String template = ErrorMessageTemplate.getTemplate("string.minLength");
            String message = ErrorMessageTemplate.formatMessage(template, 
                Map.of("minLength", minLength));
            return ValidationResult.failure(
                "",
                "string.minLength",
                message,
                strValue
            );
        }
        
        // Format validation with custom message
        if (format != null) {
            ValidationResult formatResult = validateFormat(strValue, format);
            if (!formatResult.isValid()) {
                return formatResult;
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Validate string format
     */
    private ValidationResult validateFormat(String value, String format) {
        return switch (format) {
            case "email" -> validateEmail(value);
            case "uuid" -> validateUuid(value);
            case "date" -> validateDate(value);
            case "date-time" -> validateDateTime(value);
            case "uri", "url" -> validateUri(value);
            case "hostname" -> validateHostname(value);
            case "ipv4" -> validateIpv4(value);
            case "ipv6" -> validateIpv6(value);
            default -> ValidationResult.success(); // Unknown format, skip validation
        };
    }
    
    private ValidationResult validateEmail(String value) {
        if (!value.contains("@") || !value.contains(".")) {
            String message = ErrorMessageTemplate.getTemplate("string.format.email");
            return ValidationResult.failure(
                "",
                "format.email",
                message,
                value
            );
        }
        return ValidationResult.success();
    }
    
    private ValidationResult validateUuid(String value) {
        String uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        if (!value.toLowerCase().matches(uuidRegex)) {
            return ValidationResult.failure("Invalid UUID format: " + value);
        }
        return ValidationResult.success();
    }
    
    private ValidationResult validateDate(String value) {
        // ISO 8601 date format: YYYY-MM-DD
        String dateRegex = "^\\d{4}-\\d{2}-\\d{2}$";
        if (!value.matches(dateRegex)) {
            return ValidationResult.failure("Invalid date format (expected YYYY-MM-DD): " + value);
        }
        return ValidationResult.success();
    }
    
    private ValidationResult validateDateTime(String value) {
        // ISO 8601 datetime format: YYYY-MM-DDTHH:mm:ss or with timezone
        if (!value.contains("T")) {
            return ValidationResult.failure("Invalid date-time format (expected ISO 8601): " + value);
        }
        return ValidationResult.success();
    }
    
    private ValidationResult validateUri(String value) {
        if (!value.contains("://") && !value.startsWith("/")) {
            return ValidationResult.failure("Invalid URI format: " + value);
        }
        return ValidationResult.success();
    }
    
    private ValidationResult validateHostname(String value) {
        String hostnameRegex = "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
        if (!value.matches(hostnameRegex)) {
            return ValidationResult.failure("Invalid hostname format: " + value);
        }
        return ValidationResult.success();
    }
    
    private ValidationResult validateIpv4(String value) {
        String ipv4Regex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        if (!value.matches(ipv4Regex)) {
            return ValidationResult.failure("Invalid IPv4 format: " + value);
        }
        return ValidationResult.success();
    }
    
    private ValidationResult validateIpv6(String value) {
        // Simplified IPv6 validation
        if (!value.contains(":")) {
            return ValidationResult.failure("Invalid IPv6 format: " + value);
        }
        return ValidationResult.success();
    }
    
    @Override
    public String toString() {
        return "StringValidator{" +
               "format='" + format + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}