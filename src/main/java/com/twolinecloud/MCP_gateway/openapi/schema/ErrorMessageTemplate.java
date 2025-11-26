package com.twolinecloud.MCP_gateway.openapi.schema;

import java.util.HashMap;
import java.util.Map;

/**
 * Error Message Templates
 * 에러 메시지 템플릿 관리
 */
public class ErrorMessageTemplate {
    
    private static final Map<String, String> TEMPLATES = new HashMap<>();
    
    static {
        // 기본 메시지 템플릿
        TEMPLATES.put("required", "필수 항목입니다");
        TEMPLATES.put("type.string", "문자열이어야 합니다");
        TEMPLATES.put("type.number", "숫자여야 합니다");
        TEMPLATES.put("type.integer", "정수여야 합니다");
        TEMPLATES.put("type.boolean", "불리언(true/false)이어야 합니다");
        TEMPLATES.put("type.array", "배열이어야 합니다");
        TEMPLATES.put("type.object", "객체여야 합니다");
        
        // String 관련
        TEMPLATES.put("string.minLength", "최소 {minLength}자 이상이어야 합니다");
        TEMPLATES.put("string.maxLength", "최대 {maxLength}자 이하여야 합니다");
        TEMPLATES.put("string.pattern", "형식이 올바르지 않습니다");
        TEMPLATES.put("string.format.email", "올바른 이메일 형식이 아닙니다");
        TEMPLATES.put("string.format.uuid", "올바른 UUID 형식이 아닙니다");
        TEMPLATES.put("string.format.date", "올바른 날짜 형식(YYYY-MM-DD)이 아닙니다");
        TEMPLATES.put("string.format.uri", "올바른 URI 형식이 아닙니다");
        
        // Number 관련
        TEMPLATES.put("number.minimum", "최소값은 {minimum}입니다");
        TEMPLATES.put("number.maximum", "최대값은 {maximum}입니다");
        
        // Array 관련
        TEMPLATES.put("array.minItems", "최소 {minItems}개 이상의 항목이 필요합니다");
        TEMPLATES.put("array.maxItems", "최대 {maxItems}개 이하의 항목만 허용됩니다");
        TEMPLATES.put("array.uniqueItems", "중복된 항목이 있습니다");
        TEMPLATES.put("array.items", "배열 항목이 올바르지 않습니다");
        
        // Object 관련
        TEMPLATES.put("object.property", "속성이 올바르지 않습니다");
        TEMPLATES.put("object.additionalProperties", "허용되지 않은 속성입니다");
        
        // Enum 관련
        TEMPLATES.put("enum", "허용된 값: {values}");
    }
    
    /**
     * Get template message
     */
    public static String getTemplate(String code) {
        return TEMPLATES.getOrDefault(code, "유효하지 않은 값입니다");
    }
    
    /**
     * Format message with parameters
     */
    public static String formatMessage(String template, Map<String, Object> params) {
        String message = template;
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                message = message.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return message;
    }
    
    /**
     * Add custom template
     */
    public static void addTemplate(String code, String template) {
        TEMPLATES.put(code, template);
    }
}